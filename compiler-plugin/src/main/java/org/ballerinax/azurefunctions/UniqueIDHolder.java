package org.ballerinax.azurefunctions;

/**
 * Responsible for generating unique identifier for params.
 *
 * @since 2.0.0
 */
public class UniqueIDHolder {

    private int currentIndex = 0;

    public int getNextValue() {
        return this.currentIndex++;
    }
}
