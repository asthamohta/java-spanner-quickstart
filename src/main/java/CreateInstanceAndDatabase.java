import com.google.api.gax.longrunning.OperationFuture;
import com.google.cloud.spanner.*;
import com.google.spanner.admin.database.v1.CreateDatabaseMetadata;
import com.google.spanner.admin.database.v1.UpdateDatabaseDdlMetadata;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
public class CreateInstanceAndDatabase {
    public static void main(String[] args) {
        String projectId = "banking-project";
        String instanceId = "banking";
        String databaseId = "customers";

        SpannerOptions options = SpannerOptions.newBuilder().setEmulatorHost("localhost:9010").build();
        Spanner spanner = options.getService();
        DatabaseAdminClient dbAdminClient = spanner.getDatabaseAdminClient();

        // Creating a Database
        final Database databaseToCreate = dbAdminClient
                .newDatabaseBuilder(DatabaseId.of(projectId, instanceId, databaseId))
                .build();
        final OperationFuture<Database, CreateDatabaseMetadata> operationDatabase = dbAdminClient
                .createDatabase(databaseToCreate, Arrays.asList(
                        "CREATE TABLE Customers ("
                                + "  CustomerId   INT64 NOT NULL,"
                                + "  Username STRING(100) NOT NULL,"
                                + "  Password STRING(100) NOT NULL,"
                                + "  FirstName  STRING(1024),"
                                + "  LastName   STRING(1024),"
                                + "  MobileNumber STRING(15),"
                                + ") PRIMARY KEY (CustomerId)",
                        "CREATE UNIQUE INDEX UniqueUsername ON Customers(Username) STORING (Password)"
                ));
        try {
            // Initiate the request which returns an OperationFuture.
            Database database = operationDatabase.get();
            System.out.println("Created database [" + database.getId() + "]");
        } catch (ExecutionException e) {
            // If the operation failed during execution, expose the cause.
            throw (SpannerException) e.getCause();
        } catch (InterruptedException e) {
            // Throw when a thread is waiting, sleeping, or otherwise occupied,
            // and the thread is interrupted, either before or during the activity.
            throw SpannerExceptionFactory.propagateInterrupt(e);
        }

        String schemaAccounts = "CREATE TABLE Account (" +
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

        DatabaseId database= DatabaseId.of(options.getProjectId(), instanceId, databaseId);
        OperationFuture<Void, UpdateDatabaseDdlMetadata> operation =
                dbAdminClient.updateDatabaseDdl(
                        database.getInstanceId().getInstance(),
                        database.getDatabase(),
                        Collections.singleton(schemaAccounts),
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
}
