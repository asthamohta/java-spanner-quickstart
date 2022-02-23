import com.google.api.gax.longrunning.OperationFuture;
import com.google.cloud.spanner.*;
import com.google.spanner.admin.database.v1.CreateDatabaseMetadata;
import com.google.spanner.admin.instance.v1.CreateInstanceMetadata;

import java.util.Collections;
import java.util.concurrent.ExecutionException;

public class quickstart {
    public static void main(String[] args) {
        String projectId = "span-cloud-testing";
        String instanceId = "quickstart-instance";
        String databaseId = "quickstart-database";
        String configId = "regional-us-central1";
        String displayName = "quickstart project";
        int nodeCount = 1;

        SpannerOptions options = SpannerOptions.newBuilder().build();
        Spanner spanner = options.getService();
        InstanceAdminClient instanceAdminClient = spanner.getInstanceAdminClient();
        DatabaseAdminClient dbAdminClient = spanner.getDatabaseAdminClient();

        // Creating an Instance
        InstanceInfo instanceInfo =
                InstanceInfo.newBuilder(InstanceId.of(projectId, instanceId))
                        .setInstanceConfigId(InstanceConfigId.of(projectId, configId))
                        .setNodeCount(nodeCount)
                        .setDisplayName(displayName)
                        .build();
        OperationFuture<Instance, CreateInstanceMetadata> operationInstance =
                instanceAdminClient.createInstance(instanceInfo);
        try {
            // Wait for the createInstance operation to finish.
            Instance instance = operationInstance.get();
            System.out.printf("Instance %s was successfully created%n", instance.getId());
        } catch (ExecutionException e) {
            System.out.printf(
                    "Error: Creating instance %s failed with error message %s%n",
                    instanceInfo.getId(), e.getMessage());
        } catch (InterruptedException e) {
            System.out.println("Error: Waiting for createInstance operation to finish was interrupted");
        }

        // Creating a Database
        final Database databaseToCreate = dbAdminClient
                .newDatabaseBuilder(DatabaseId.of(projectId, instanceId, databaseId))
                .build();
        final OperationFuture<Database, CreateDatabaseMetadata> operationDatabase = dbAdminClient
                .createDatabase(databaseToCreate, Collections.singleton(
                        "CREATE TABLE Singers ("
                                + "  SingerId   INT64 NOT NULL,"
                                + "  FirstName  STRING(1024),"
                                + "  LastName   STRING(1024),"
                                + "  SingerInfo BYTES(MAX)"
                                + ") PRIMARY KEY (SingerId)"
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

        // Insert and Read Data
        DatabaseId db = DatabaseId.of(options.getProjectId(), instanceId, databaseId);
        DatabaseClient dbClient = spanner.getDatabaseClient(db);
        dbClient
                .readWriteTransaction()
                .run(transaction -> {
                    // Insert record.
                    String sql =
                            "INSERT INTO Singers (SingerId, FirstName, LastName) "
                                    + " VALUES (11, 'Timothy', 'Campbell')";
                    long rowCount = transaction.executeUpdate(Statement.of(sql));
                    System.out.printf("%d record inserted.\n", rowCount);
                    // Read newly inserted record.
                    sql = "SELECT FirstName, LastName FROM Singers WHERE SingerId = 11";
                    // We use a try-with-resource block to automatically release resources held by
                    // ResultSet.
                    try (ResultSet resultSet = transaction.executeQuery(Statement.of(sql))) {
                        while (resultSet.next()) {
                            System.out.printf(
                                    "%s %s\n",
                                    resultSet.getString("FirstName"), resultSet.getString("LastName"));
                        }
                    }
                    return null;
                });
    }
}
