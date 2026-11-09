package com.asad.breaktime.data

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Prices as Google Play shows them in the user's currency, e.g. "$2.99" or "Rs 850". */
data class PremiumPrices(
    val monthly: String?,
    val yearly: String?,
    val monthlyTrialDays: Int,
    val yearlyTrialDays: Int,
)

/**
 * Google Play Billing for BreakTime Premium.
 *
 * Play Console setup (Monetize → Subscriptions):
 *  - Product ID  [PRODUCT_ID]
 *  - Base plans  [PLAN_MONTHLY] (1 month, auto-renewing) and [PLAN_YEARLY] (1 year, auto-renewing)
 *  - Optional free-trial offers on either plan; the app picks them up automatically.
 */
object Billing {
    const val PRODUCT_ID = "breaktime_premium"
    const val PLAN_MONTHLY = "monthly"
    const val PLAN_YEARLY = "yearly"

    private var client: BillingClient? = null
    private var store: BreakStore? = null
    private var details: ProductDetails? = null

    private val _prices = MutableStateFlow<PremiumPrices?>(null)
    val prices: StateFlow<PremiumPrices?> = _prices

    /** Connects to Google Play and refreshes whether the user is Premium. Safe to call often. */
    fun refresh(context: Context) {
        val app = context.applicationContext
        if (store == null) store = BreakStore.get(app)
        val c = client ?: BillingClient.newBuilder(app)
            .setListener { result, purchases -> onPurchases(result, purchases) }
            .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
            .build()
            .also { client = it }
        if (c.isReady) {
            loadAll(c)
            return
        }
        c.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingResponseCode.OK) loadAll(c)
            }

            override fun onBillingServiceDisconnected() {
                // Reconnects on the next refresh() (every time the app comes to the front).
            }
        })
    }

    private fun loadAll(c: BillingClient) {
        queryPurchases(c) {}
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRODUCT_ID)
                        .setProductType(ProductType.SUBS)
                        .build()
                )
            )
            .build()
        c.queryProductDetailsAsync(params) { result, list ->
            if (result.responseCode != BillingResponseCode.OK) return@queryProductDetailsAsync
            val pd = list.firstOrNull() ?: return@queryProductDetailsAsync
            details = pd
            _prices.value = PremiumPrices(
                monthly = basePrice(pd, PLAN_MONTHLY),
                yearly = basePrice(pd, PLAN_YEARLY),
                monthlyTrialDays = trialDays(pd, PLAN_MONTHLY),
                yearlyTrialDays = trialDays(pd, PLAN_YEARLY),
            )
        }
    }

    private fun offers(pd: ProductDetails, plan: String) =
        pd.subscriptionOfferDetails.orEmpty().filter { it.basePlanId == plan }

    /** The plan's regular price (the last phase is the recurring one). */
    private fun basePrice(pd: ProductDetails, plan: String): String? =
        offers(pd, plan).firstOrNull { it.offerId == null }
            ?.pricingPhases?.pricingPhaseList?.lastOrNull()?.formattedPrice

    /** Best offer for a new subscriber: a free trial if the user is eligible, else the base plan. */
    private fun bestOffer(pd: ProductDetails, plan: String) =
        offers(pd, plan).maxByOrNull { trialDaysOf(it) }

    private fun trialDays(pd: ProductDetails, plan: String): Int =
        bestOffer(pd, plan)?.let { trialDaysOf(it) } ?: 0

    private fun trialDaysOf(offer: ProductDetails.SubscriptionOfferDetails): Int {
        val free = offer.pricingPhases.pricingPhaseList.firstOrNull { it.priceAmountMicros == 0L } ?: return 0
        // ISO 8601 period such as P7D, P1W or P1M.
        val m = Regex("P(\\d+)([DWM])").find(free.billingPeriod) ?: return 0
        val n = m.groupValues[1].toInt()
        return when (m.groupValues[2]) {
            "W" -> n * 7
            "M" -> n * 30
            else -> n
        }
    }

    /**
     * Opens Google Play's purchase sheet. Returns false if Premium isn't available yet
     * (no connection, or the subscription isn't set up in Play Console).
     */
    fun buy(activity: Activity, yearly: Boolean): Boolean {
        val c = client ?: return false
        val pd = details ?: return false
        val offer = bestOffer(pd, if (yearly) PLAN_YEARLY else PLAN_MONTHLY) ?: return false
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(pd)
                        .setOfferToken(offer.offerToken)
                        .build()
                )
            )
            .build()
        return c.launchBillingFlow(activity, params).responseCode == BillingResponseCode.OK
    }

    /** Checks Google Play for an active subscription. Calls back with whether one was found. */
    fun restore(context: Context, onResult: (Boolean?) -> Unit) {
        refresh(context)
        val c = client
        if (c == null || !c.isReady) {
            onResult(null)
            return
        }
        queryPurchases(c, onResult)
    }

    private fun queryPurchases(c: BillingClient, onResult: (Boolean) -> Unit) {
        val params = QueryPurchasesParams.newBuilder().setProductType(ProductType.SUBS).build()
        c.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode != BillingResponseCode.OK) return@queryPurchasesAsync
            val active = handle(purchases)
            store?.isPremium = active
            onResult(active)
        }
    }

    private fun onPurchases(result: BillingResult, purchases: List<Purchase>?) {
        if (result.responseCode == BillingResponseCode.OK && purchases != null) {
            if (handle(purchases)) store?.isPremium = true
        }
    }

    /** Acknowledges new purchases (Google refunds them after 3 days otherwise). Returns true if Premium is active. */
    private fun handle(purchases: List<Purchase>): Boolean {
        var active = false
        for (p in purchases) {
            if (PRODUCT_ID !in p.products || p.purchaseState != Purchase.PurchaseState.PURCHASED) continue
            active = true
            if (!p.isAcknowledged) {
                client?.acknowledgePurchase(
                    AcknowledgePurchaseParams.newBuilder().setPurchaseToken(p.purchaseToken).build()
                ) {}
            }
        }
        return active
    }
}
