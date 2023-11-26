package com.amazonaws.services.dynamodbv2;

import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Optional;

import com.amazonaws.services.dynamodbv2.AcquireLockOptions.AcquireLockOptionsBuilder;
import com.amazonaws.services.dynamodbv2.GetLockOptions.GetLockOptionsBuilder;
import com.amazonaws.services.dynamodbv2.MongoDBLockClientOptions.MongoDBLockClientOptionsBuilder;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.ReadPreference;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

public class Main {
    protected static final Log logger = LogFactory.getLog(Main.class);
    private static MongoClient mongo;
    public static void main(String[] args) {
        // Connect to MongoDB

        MongoClientSettings settings = MongoClientSettings.builder()
                                            .readPreference(ReadPreference.nearest())
                                            .applyConnectionString(new ConnectionString("mongodb://localhost:27017"))
                                            .build();
        mongo = MongoClients.create(settings);

        boolean exists = false;
        for (String name : mongo.getDatabase("lockdb").listCollectionNames()) {
            exists = name.equals("locks");
                
            if (exists)
                break;
        }
         
        if (!exists)
            mongo.getDatabase("lockdb").createCollection("locks");

        MongoDBLockClientOptionsBuilder builder = new MongoDBLockClientOptionsBuilder(mongo)
                                                        .withCreateHeartbeatBackgroundThread(true)
                                                        .withHeartbeatPeriod(5L)
                                                        .withLeaseDuration(100L)
                                                        .withOwnerName("John Doe")
                                                        .withTimeUnit(TimeUnit.SECONDS);

        MongoDBLockClient client = new MongoDBLockClient(builder.build());

        AcquireLockOptionsBuilder acquireLockOptionsBuilder = new AcquireLockOptionsBuilder("asdf")
                                                            .withAcquireOnlyIfLockAlreadyExists(false)
                                                            .withDeleteLockOnRelease(false)
                                                            .withReentrant(false);

        GetLockOptionsBuilder getLockOptionsBuilder = new GetLockOptionsBuilder("asdf");
                                        
        try {
            LockItem put = client.acquireLock(acquireLockOptionsBuilder.build());
            
            Optional<LockItem> optional = client.getLock(getLockOptionsBuilder.build());
            LockItem get = optional.isPresent() ? optional.get() : null;

            logger.info("Put LockItem: " + put.toString());
            logger.info("Get LockItem: " + get.toString());

            ReleaseLockOptions release = new ReleaseLockOptions(get, true, true, Optional.empty());
            
            logger.info("Release lock on '" + get.getPartitionKey() + "': " + client.releaseLock(release));

        } catch (InterruptedException ex) {
            ex.printStackTrace();
            return;
        }
    }
}
