import com.google.api.gax.longrunning.OperationFuture;
import com.google.cloud.spanner.*;
import com.google.spanner.admin.database.v1.UpdateDatabaseDdlMetadata;
import java.util.Collections;
import java.util.concurrent.ExecutionException;

public class Schema {
  static void addAccountTable(DatabaseAdminClient adminClient, DatabaseId databaseId, String schema) {
    OperationFuture<Void, UpdateDatabaseDdlMetadata> operation =
            adminClient.updateDatabaseDdl(
                    databaseId.getInstanceId().getInstance(),
                    databaseId.getDatabase(),
                    Collections.singleton(schema),
                    null);
    try {
      // Initiate the request which returns an OperationFuture.
      operation.get();
      System.out.println("Added Account Table");
    } catch (ExecutionException e) {
      // If the operation failed during execution, expose the cause.
      throw (SpannerException) e.getCause();
    } catch (InterruptedException e) {
      // Throw when a thread is waiting, sleeping, or otherwise occupied,
      // and the thread is interrupted, either before or during the activity.
      throw SpannerExceptionFactory.propagateInterrupt(e);
    }
  }

  public static void main(String[] args) {
    SpannerOptions options = SpannerOptions.newBuilder().setEmulatorHost("localhost:9010").build();
    Spanner spanner = options.getService();

    String instanceId = "banking";
    String databaseId = "customers";
    DatabaseId db= DatabaseId.of(options.getProjectId(), instanceId, databaseId);

    DatabaseAdminClient dbAdminClient = spanner.getDatabaseAdminClient();

    String schema = "CREATE TABLE Account (" +
            "  CustomerId INT64 NOT NULL," +
            "  AccountId INT64 NOT NULL," +
            "  CreatedOn DATE," +
            "  Address JSON," +
            "  Photo BYTES(MAX)," +
            "  LastTransactionTime TIMESTAMP," +
            "  SavingsAccount BOOL," +
            "  Balance NUMERIC," +
            "  RecentTransactionTimestamps ARRAY<TIMESTAMP>" +
            ") PRIMARY KEY (CustomerId, AccountId)," +
            "INTERLEAVE IN PARENT Customers ON DELETE CASCADE";

    addAccountTable(dbAdminClient, db, schema);
  }
}
