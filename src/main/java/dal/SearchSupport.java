package dal;

/** PostgreSQL search fragments that treat Vietnamese accents as equivalent. */
final class SearchSupport {
    private static final String FROM =
            "àáạảãâầấậẩẫăằắặẳẵđèéẹẻẽêềếệểễìíịỉĩòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹ";
    private static final String TO =
            "aaaaaaaaaaaaaaaaadeeeeeeeeeeeiiiiiooooooooooooooooouuuuuuuuuuuyyyyy";

    private SearchSupport() {}

    static String contains(String expression) {
        return fold(expression) + " LIKE " + fold("?");
    }

    private static String fold(String expression) {
        return "translate(lower(" + expression + "), '" + FROM + "', '" + TO + "')";
    }
}
