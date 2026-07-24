package dal;

/** SQL Server search fragments that treat Vietnamese accents as equivalent. */
final class SearchSupport {
    private SearchSupport() {}

    static String contains(String expression) {
        return expression + " COLLATE Vietnamese_CI_AI LIKE ? COLLATE Vietnamese_CI_AI";
    }
}
