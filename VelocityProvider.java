import java.time.Duration;

interface VelocityProvider {

    /**
     * This method is called during the payment risk assessment.
     *
     * It returns how many times the card in the Payment has been seen in the last minutes/seconds/hours as
     * defined in the {@code duration} parameter at the time the payment is being processed.
     *
     * @param payment  The payment being processed
     * @param duration The interval to count
     * @return The number of times the card was used in the interval defined in duration.
     */
    int getCardUsageCount(Solution.Payment payment, Duration duration);


    /**
     * After the payment is processed this method is called.
     *
     * @param payment The payment that has been processed.
     */
    void registerPayment(Solution.Payment payment);

    /**
     * @return Instance of a Velocity provider
     */
    static VelocityProvider getProvider() {
        return VelocityProviderImpl.getProvider();
    }
}
