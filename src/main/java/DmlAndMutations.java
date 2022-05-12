import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.DatabaseId;
import com.google.cloud.spanner.Mutation;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.Spanner;
import com.google.cloud.spanner.SpannerOptions;
import com.google.cloud.spanner.Statement;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

public class DmlAndMutations {
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

  public static BigDecimal getBalance(DatabaseClient dbClient, String CustomerId, String AccountId){
    Statement statement =
        Statement.newBuilder(
                "SELECT Balance FROM Account WHERE CustomerId = @customerId "
                    + "AND AccountId = @accountId")
            .bind("customerId")
            .to(CustomerId)
            .bind("accountId")
            .to(AccountId)
            .build();
    BigDecimal Balance = new BigDecimal("0");
    try (ResultSet resultSet = dbClient.singleUse().executeQuery(statement)) {
      while (resultSet.next()) {
        Balance = resultSet.getBigDecimal("Balance");
      }
    }
    return Balance;
  }
  public static void insertDataUsingDmlInAccountsTable(DatabaseClient dbClient, String CustomerId){
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

  public static void insertDataUsingMutationsInLedgersTable(DatabaseClient dbClient, String CustomerId){
    Scanner input = new Scanner(System.in);
    System.out.println("Enter Account Number: ");
    String AccountId = input.nextLine();
    System.out.println("Enter Transaction Details: ");
    String Details = input.nextLine();
    System.out.println("Enter Debit or Credit Amount");
    BigDecimal Amount = new BigDecimal(input.nextLine());
    String TransactionId = String.valueOf(UUID.randomUUID());
    List<Mutation> mutations =
        Arrays.asList(
            Mutation.newInsertBuilder("Ledger")
                .set("CustomerId")
                .to(CustomerId)
                .set("AccountId")
                .to(AccountId)
                .set("TransactionId")
                .to(TransactionId)
                .set("Date")
                .to(java.time.LocalDate.now().toString())
                .set("Amount")
                .to(Amount)
                .set("Details")
                .to(Details)
                .build(),
            Mutation.newUpdateBuilder("Account")
                .set("CustomerId")
                .to(CustomerId)
                .set("AccountId")
                .to(AccountId)
                .set("Balance")
                .to(getBalance(dbClient, CustomerId, AccountId).add(Amount))
                .build()
        );
    dbClient.write(mutations);
    System.out.printf("Record inserted in LedgerId and Account Balance Updated");
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

    System.out.println("To enter Data into Accounts Table enter 1 \n"
        + "To enter Data into LedgersTable enter 2");
    int option = input.nextInt();
    input.nextLine();

    if(option == 1)
    insertDataUsingDmlInAccountsTable(dbClient, getCustomerId(dbClient, username, password));
    else if(option == 2)
    insertDataUsingMutationsInLedgersTable(dbClient, getCustomerId(dbClient, username, password));
  }
}
