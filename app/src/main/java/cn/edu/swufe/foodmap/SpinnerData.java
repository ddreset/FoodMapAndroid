package cn.edu.swufe.foodmap;

/**
 * Created by user on 2017/6/11.
 */

public class SpinnerData {

    private String value = "";
    private String text = "";

    public SpinnerData() {
        value = "";
        text = "";
    }

    public SpinnerData(String _value, String _text) {
        value = _value;
        text = _text;
    }

    @Override
    public String toString() {

        return text;
    }

    public String getValue() {
        return value;
    }

    public String getText() {
        return text;
    }
}