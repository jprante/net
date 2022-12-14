package org.xbib.net.template.vars.values;

/**
 * Null value.
 */
public class NullValue extends VariableValue {

    public NullValue() {
        super(ValueType.NULL);
    }

    @Override
    public boolean isEmpty() {
        return true;
    }
}
