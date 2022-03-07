import com.google.cloud.spanner.*;

import java.util.Scanner;

public class InsertAndReadData {
    public static void createCustomer(DatabaseClient dbClient, String customerId, String FirstName, String LastName, String MobileNumber){
        dbClient
                .readWriteTransaction()
                .run(transaction -> {
                    String sql =
                            String.format("INSERT INTO Customers (CustomerId, FirstName, LastName, MobileNumber) "
                                    + " VALUES (%s, '%s', '%s', '%s')", customerId,
                                    FirstName, LastName, MobileNumber);
                    long rowCount = transaction.executeUpdate(Statement.of(sql));
                    System.out.printf("%d record inserted.\n", rowCount);
                    return null;
                });
    }

    public static void authenticateData(DatabaseClient dbClient, String customerId){
        dbClient
                .readWriteTransaction()
                .run(transaction -> {
                    // Read inserted record.
                    String sql = String.format("SELECT FirstName, LastName, MobileNumber FROM Customers WHERE CustomerId = %s", customerId);
                    // We use a try-with-resource block to automatically release resources held by ResultSet.
                    try (ResultSet resultSet = transaction.executeQuery(Statement.of(sql))) {
                        while (resultSet.next()) {
                            System.out.printf(
                                    "Customer found as: %s %s %s\n",
                                    resultSet.getString("FirstName"), resultSet.getString("LastName"), resultSet.getString("MobileNumber"));
                        }
                    }
                    return null;
                });
    }

    public static void main(String[] args) {
        String instanceId = "quickstart-instance";
        String databaseId = "quickstart-database";

        SpannerOptions options = SpannerOptions.newBuilder().build();
        Spanner spanner = options.getService();

        Scanner input = new Scanner(System.in);
        System.out.println("Enter Customer Id: ");
        String customerId = input.nextLine();
        System.out.println("Enter Customer First Name: ");
        String customerFirstName = input.nextLine();
        System.out.println("Enter Customer Last Name: ");
        String customerLastName = input.nextLine();
        System.out.println("Enter Customer Mobile Number: ");
        String customerMobileNumber = input.nextLine();

        // Insert and Read Data
        DatabaseId db = DatabaseId.of(options.getProjectId(), instanceId, databaseId);
        DatabaseClient dbClient = spanner.getDatabaseClient(db);
        createCustomer(dbClient, customerId, customerFirstName, customerLastName, customerMobileNumber);
        authenticateData(dbClient, customerId);

    }
}
