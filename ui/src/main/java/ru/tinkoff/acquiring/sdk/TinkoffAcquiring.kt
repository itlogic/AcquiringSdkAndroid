/*
 * Copyright © 2020 Tinkoff Bank
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package ru.tinkoff.acquiring.sdk

import androidx.fragment.app.FragmentActivity
import ru.tinkoff.acquiring.sdk.localization.LocalizationSource
import ru.tinkoff.acquiring.sdk.models.AsdkState
import ru.tinkoff.acquiring.sdk.models.CollectDataState
import ru.tinkoff.acquiring.sdk.models.DefaultState
import ru.tinkoff.acquiring.sdk.models.PaymentSource
import ru.tinkoff.acquiring.sdk.models.options.FeaturesOptions
import ru.tinkoff.acquiring.sdk.models.options.screen.AttachCardOptions
import ru.tinkoff.acquiring.sdk.models.options.screen.BaseAcquiringOptions
import ru.tinkoff.acquiring.sdk.models.options.screen.PaymentOptions
import ru.tinkoff.acquiring.sdk.models.options.screen.SavedCardsOptions
import ru.tinkoff.acquiring.sdk.models.paysources.AttachedCard
import ru.tinkoff.acquiring.sdk.models.paysources.CardData
import ru.tinkoff.acquiring.sdk.models.paysources.GooglePay
import ru.tinkoff.acquiring.sdk.payment.PaymentProcess
import ru.tinkoff.acquiring.sdk.ui.activities.AttachCardActivity
import ru.tinkoff.acquiring.sdk.ui.activities.BaseAcquiringActivity
import ru.tinkoff.acquiring.sdk.ui.activities.PaymentActivity
import ru.tinkoff.acquiring.sdk.ui.activities.SavedCardsActivity
import ru.tinkoff.acquiring.sdk.ui.activities.StaticQrActivity
import ru.tinkoff.acquiring.sdk.ui.activities.ThreeDsActivity

/**
 * Точка входа для взаимодействия с Acquiring SDK
 *
 * @param terminalKey ключ терминала. Выдается после подключения к Tinkoff Acquiring
 * @param password    пароль терминала. Выдается вместе с terminalKey
 * @param publicKey   экземпляр PublicKey созданный из публичного ключа, выдаваемого вместе с
 *                    terminalKey
 *
 * @author Mariya Chernyadieva
 */
class TinkoffAcquiring(
        private val terminalKey: String,
        private val password: String,
        private val publicKey: String
) {
    private val sdk = AcquiringSdk(terminalKey, password, publicKey)

    /**
     * Создает платежную сессию. Для проведения оплаты с помощью привязанной карты.
     * Включает в себя инициирование нового платежа и подтверждение платежа.
     * Процесс асинхронный
     *
     * @param attachedCard   привязанная карта
     * @param paymentOptions настройки платежной сессии
     * @return объект для проведения оплаты
     */
    fun initPayment(attachedCard: AttachedCard, paymentOptions: PaymentOptions): PaymentProcess {
        return PaymentProcess(sdk).createPaymentProcess(attachedCard, paymentOptions)
    }

    /**
     * Создает платежную сессию. Для проведения оплаты с помощью карты.
     * Включает в себя инициирование нового платежа и подтверждение платежа.
     * Процесс асинхронный
     *
     * @param cardData       данные карты
     * @param paymentOptions настройки платежной сессии
     * @return объект для проведения оплаты
     */
    fun initPayment(cardData: CardData, paymentOptions: PaymentOptions): PaymentProcess {
        return PaymentProcess(sdk).createPaymentProcess(cardData, paymentOptions)
    }

    /**
     * Создает платежную сессию. Для проведения оплаты с помощью Google Pay.
     * Включает в себя инициирование нового платежа и подтверждение платежа
     * Процесс асинхронный
     *
     * @param googlePayToken токен для оплаты полученный через Google Pay
     * @param paymentOptions настройки платежной сессии
     * @return объект для проведения оплаты
     */
    fun initPayment(googlePayToken: String, paymentOptions: PaymentOptions): PaymentProcess {
        return PaymentProcess(sdk).createPaymentProcess(GooglePay(googlePayToken), paymentOptions)
    }

    /**
     * Создает платежную сессию для подтверждения ранее инициированного платежа.
     * Включает в себя только подтверждение платежа
     * Процесс асинхронный
     *
     * @param paymentId     уникальный идентификатор транзакции в системе банка,
     *                      полученный после проведения инициации платежа
     * @param paymentSource источник платежа
     * @return объект для проведения оплаты
     */
    fun finishPayment(paymentId: Long, paymentSource: PaymentSource): PaymentProcess {
        return PaymentProcess(sdk).createFinishProcess(paymentId, paymentSource)
    }

    /**
     * Запуск экрана Acquiring SDK для проведения оплаты
     *
     * @param activity    контекст для запуска экрана
     * @param options     настройки платежной сессии
     * @param requestCode код для получения результата, по завершению работы экрана Acquiring SDK
     * @param state       вспомогательный параметр для запуска экрана Acquiring SDK
     *                    с заданного состояния
     */
    fun openPaymentScreen(activity: FragmentActivity, options: PaymentOptions, requestCode: Int,
                          state: AsdkState = DefaultState) {
        if (state is CollectDataState) {
            state.data.putAll(ThreeDsActivity.collectData(activity, state.response))
        } else {
            options.asdkState = state
            options.setTerminalParams(terminalKey, password, publicKey)
            val intent = BaseAcquiringActivity.createIntent(activity, options, PaymentActivity::class.java)
            activity.startActivityForResult(intent, requestCode)
        }
    }

    /**
     * Запуск экрана Acquiring SDK для привязки новой карты
     *
     * @param activity    контекст для запуска экрана
     * @param options     настройки привязки карты
     * @param requestCode код для получения результата, по завершению работы экрана Acquiring SDK
     */
    fun openAttachCardScreen(activity: FragmentActivity, options: AttachCardOptions, requestCode: Int) {
        options.setTerminalParams(terminalKey, password, publicKey)
        val intent = BaseAcquiringActivity.createIntent(activity, options, AttachCardActivity::class.java)
        activity.startActivityForResult(intent, requestCode)
    }

    /**
     * Запуск экрана Acquiring SDK для просмотра сохраненных карт
     *
     * @param activity          контекст для запуска экрана
     * @param savedCardsOptions настройки экрана сохраненных карт
     * @param requestCode       код для получения результата, по завершению работы экрана Acquiring SDK
     */
    fun openSavedCardsScreen(activity: FragmentActivity, savedCardsOptions: SavedCardsOptions, requestCode: Int) {
        savedCardsOptions.setTerminalParams(terminalKey, password, publicKey)
        val intent = BaseAcquiringActivity.createIntent(activity, savedCardsOptions, SavedCardsActivity::class.java)
        activity.startActivityForResult(intent, requestCode)
    }

    /**
     * Запуск экрана с отображением QR кода для оплаты покупателем
     *
     * @param activity        контекст для запуска экрана
     * @param featuresOptions конфигурация визуального отображения экрана
     * @param requestCode     код для получения результата, по завершению работы экрана Acquiring SDK
     */
    fun openStaticQrScreen(activity: FragmentActivity, featuresOptions: FeaturesOptions, requestCode: Int) {
        val options = BaseAcquiringOptions().apply {
            setTerminalParams(
                    this@TinkoffAcquiring.terminalKey,
                    this@TinkoffAcquiring.password,
                    this@TinkoffAcquiring.publicKey)
            features = featuresOptions
        }
        val intent = BaseAcquiringActivity.createIntent(activity, options, StaticQrActivity::class.java)
        activity.startActivityForResult(intent, requestCode)
    }

    /**
     * Запуск экрана с отображением QR кода для оплаты покупателем
     *
     * @param activity     контекст для запуска экрана
     * @param localization локализация экрана
     * @param requestCode  код для получения результата, по завершению работы экрана Acquiring SDK
     */
    fun openStaticQrScreen(activity: FragmentActivity, localization: LocalizationSource, requestCode: Int) {
        openStaticQrScreen(activity, FeaturesOptions().apply { localizationSource = localization }, requestCode)
    }

    companion object {

        const val RESULT_ERROR = 500
        const val EXTRA_ERROR = "extra_error"
        const val EXTRA_CARD_ID = "extra_card_id"
        const val EXTRA_PAYMENT_ID = "extra_payment_id"
    }
}