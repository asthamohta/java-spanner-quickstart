import com.google.cloud.spanner.*;

import java.util.Random;
import java.util.Scanner;
import java.util.UUID;

public class InsertAndReadData {
    public static void createCustomer(DatabaseClient dbClient, String Username, String Password, String FirstName,
                                      String LastName, String MobileNumber){
        dbClient
                .readWriteTransaction()
                .run(transaction -> {
                    String customerId = String.valueOf(UUID.randomUUID());;
                    String sql =
                            String.format("INSERT INTO Customers (CustomerId, Username, Password, FirstName, LastName, " +
                                            "MobileNumber) "
                                    + " VALUES ('%s', '%s', '%s', '%s', '%s', '%s')", customerId, Username, Password,
                                    FirstName, LastName, MobileNumber);
                    long rowCount = transaction.executeUpdate(Statement.of(sql));
                    System.out.printf("%d record inserted.\n", rowCount);
                    return null;
                });
    }

    public static void login(DatabaseClient dbClient, String Username, String Password){
        dbClient
                .readWriteTransaction()
                .run(transaction -> {
                    // Read inserted record.
                    String sql =
                            String.format("SELECT FirstName, LastName, MobileNumber FROM Customers WHERE Username = '%s' " +
                                            "AND Password = '%s'",
                            Username, Password);
                    // We use a try-with-resource block to automatically release resources held by ResultSet.
                    try (ResultSet resultSet = transaction.executeQuery(Statement.of(sql))) {
                        while (resultSet.next()) {
                            System.out.printf(
                                    "Customer found as: %s %s %s\n",
                                    resultSet.getString("FirstName"), resultSet.getString("LastName"),
                                    resultSet.getString("MobileNumber"));
                        }
                    }
                    return null;
                });
    }

    public static void main(String[] args) {
        String instanceId = "banking";
        String databaseId = "customers";

        SpannerOptions options = SpannerOptions.newBuilder().setEmulatorHost("localhost:9010").build();
        Spanner spanner = options.getService();

        Scanner input = new Scanner(System.in);
        System.out.println("Would you like to login or create a new user? \n" +
                "Enter 1 for login\n"+
                "Enter 2 for sign up");
        int option = input.nextInt();
        input.nextLine();
        System.out.println("Enter Username: ");
        String username = input.nextLine();
        System.out.println("Enter Password: ");
        String password = input.nextLine();

        DatabaseId db = DatabaseId.of(options.getProjectId(), instanceId, databaseId);
        DatabaseClient dbClient = spanner.getDatabaseClient(db);

        if(option==1){
            login(dbClient, username, password);
        }
        else if(option==2){
            System.out.println("Enter Customer First Name: ");
            String customerFirstName = input.nextLine();
            System.out.println("Enter Customer Last Name: ");
            String customerLastName = input.nextLine();
            System.out.println("Enter Customer Mobile Number: ");
            String customerMobileNumber = input.nextLine();
            createCustomer(dbClient, username, password, customerFirstName, customerLastName, customerMobileNumber);
        }
    }
}
