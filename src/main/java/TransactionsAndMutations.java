import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.DatabaseId;
import com.google.cloud.spanner.Mutation;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.Spanner;
import com.google.cloud.spanner.SpannerOptions;
import com.google.cloud.spanner.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

public class TransactionsAndMutations {
  public static String getCustomerId(DatabaseClient dbClient, String Username, String Password){
    return dbClient
        .readWriteTransaction()
        .run(transaction -> {
          // Read inserted record.
          String readSql =
              String.format("SELECT CustomerId FROM Customers WHERE Username = '%s' " +
                      "AND Password = '%s'",
                  Username, Password);
          // We use a try-with-resource block to automatically release resources held by ResultSet.
          String CustomerId="";
          try (ResultSet resultSet = transaction.executeQuery(Statement.of(readSql))) {
            while (resultSet.next()) {
              CustomerId = resultSet.getString("CustomerId");
            }
          }
          return CustomerId;
        });
  }
  public static void readAndInsertDataUsingTransactions(DatabaseClient dbClient, String CustomerId){
    dbClient
        .readWriteTransaction()
        .run(transaction -> {
          // We use a try-with-resource block to automatically release resources held by ResultSet.
          String AccountId = String.valueOf(UUID.randomUUID());
          String CreatedOn = java.time.LocalDate.now().toString();
          String Balance = "0";
          String sql =
              String.format("INSERT INTO Account (CustomerId, AccountId, CreatedOn, Balance) "
                  + " VALUES ('%s', '%s', '%s', %s)"
                  , CustomerId, AccountId, CreatedOn, Balance);
          long rowCount = transaction.executeUpdate(Statement.of(sql));
          System.out.printf("%d record inserted with AccountId %s.\n", rowCount, AccountId);
          return null;
        });
  }

  public static void readAndInsertDataUsingMutations(DatabaseClient dbClient, String CustomerId){
    String AccountId = String.valueOf(UUID.randomUUID());
    List<Mutation> mutations = new ArrayList<>();
    mutations.add(
        Mutation.newInsertBuilder("Account")
            .set("CustomerId")
            .to(CustomerId)
            .set("AccountId")
            .to(AccountId)
            .set("Balance")
            .to("0")
            .set("CreatedOn")
            .to(java.time.LocalDate.now().toString())
            .build());
    dbClient.write(mutations);
    System.out.printf("Record inserted with AccountId %s.\n", AccountId);
  }

  public static void main(String[] args) {
    String instanceId = "banking";
    String databaseId = "customers";

    SpannerOptions options = SpannerOptions.newBuilder().setEmulatorHost("localhost:9010").build();
    Spanner spanner = options.getService();

    Scanner input = new Scanner(System.in);
    System.out.println("Enter Username: ");
    String username = input.nextLine();
    System.out.println("Enter Password: ");
    String password = input.nextLine();

    DatabaseId db = DatabaseId.of(options.getProjectId(), instanceId, databaseId);
    DatabaseClient dbClient = spanner.getDatabaseClient(db);

    readAndInsertDataUsingTransactions(dbClient, getCustomerId(dbClient, username, password));
    readAndInsertDataUsingMutations(dbClient, getCustomerId(dbClient, username, password));

  }
}
