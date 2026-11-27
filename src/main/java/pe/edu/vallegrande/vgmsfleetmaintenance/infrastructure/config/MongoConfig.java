package pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.CollectionOptions;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.convert.NoOpDbRefResolver;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.core.schema.MongoJsonSchema;
import org.springframework.data.mongodb.core.validation.Validator;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * MongoDB persistence configuration.
 * Disables automatic _class discriminator fields and ensures that required collections,
 * strict JSON Schema validators, and audit indexes are aligned at startup.
 */
@Slf4j
@Configuration
public class MongoConfig {

    public static final String MAINTENANCE_ORDERS_COLLECTION = "maintenance_orders";
    public static final String MAINTENANCE_ORDER_AUDITS_COLLECTION = "maintenance_order_audits";

    @Bean
    public MappingMongoConverter mappingMongoConverter(
            MongoMappingContext mappingContext,
            MongoCustomConversions conversions) {
        MappingMongoConverter converter = new MappingMongoConverter(NoOpDbRefResolver.INSTANCE, mappingContext);
        converter.setCustomConversions(conversions);
        converter.setTypeMapper(new DefaultMongoTypeMapper(null));
        return converter;
    }

    @Bean
    public ApplicationRunner mongoSchemaInitializer(ReactiveMongoTemplate mongoTemplate) {
        return args -> {
            ensureCollectionWithSchema(
                    mongoTemplate,
                    MAINTENANCE_ORDERS_COLLECTION,
                    buildMaintenanceOrdersSchema())
                    .then(ensureCollectionWithSchema(
                            mongoTemplate,
                            MAINTENANCE_ORDER_AUDITS_COLLECTION,
                            buildMaintenanceOrderAuditsSchema()))
                    .then(ensureAuditIndexes(mongoTemplate))
                    .subscribe(
                            unused -> log.info("MongoDB collections and strict JSON Schema validators verified"),
                            err -> log.warn("MongoDB schema verification skipped or deferred: {}",
                                    err.getClass().getSimpleName()));
        };
    }

    private Mono<Void> ensureCollectionWithSchema(
            ReactiveMongoTemplate mongoTemplate,
            String collectionName,
            Document jsonSchema) {
        Mono<Boolean> existsMono = mongoTemplate.collectionExists(collectionName);
        if (existsMono == null) {
            return Mono.empty();
        }
        return existsMono.flatMap(exists -> {
            if (!Boolean.TRUE.equals(exists)) {
                CollectionOptions options = CollectionOptions.empty()
                        .validator(Validator.schema(MongoJsonSchema.of(jsonSchema)))
                        .strictValidation()
                        .failOnValidationError();
                Mono<?> createMono = mongoTemplate.createCollection(collectionName, options);
                return createMono != null ? createMono.then() : Mono.empty();
            }
            Document collMod = new Document("collMod", collectionName)
                    .append("validator", new Document("$jsonSchema", jsonSchema))
                    .append("validationLevel", "strict")
                    .append("validationAction", "error");
            Mono<Document> cmdMono = mongoTemplate.executeCommand(collMod);
            return cmdMono != null ? cmdMono.then() : Mono.empty();
        });
    }

    private Mono<Void> ensureAuditIndexes(ReactiveMongoTemplate mongoTemplate) {
        var indexOps = mongoTemplate.indexOps(MAINTENANCE_ORDER_AUDITS_COLLECTION);
        if (indexOps == null) {
            return Mono.empty();
        }
        Mono<String> tsIdx = indexOps.ensureIndex(new Index().on("timestamp", Sort.Direction.DESC).named("idx_audit_timestamp"));
        Mono<String> tenantIdx = indexOps.ensureIndex(new Index().on("tenant", Sort.Direction.ASC).named("idx_audit_tenant"));
        Mono<String> corrIdx = indexOps.ensureIndex(new Index().on("correlationId", Sort.Direction.ASC).named("idx_audit_correlation_id"));
        if (tsIdx == null || tenantIdx == null || corrIdx == null) {
            return Mono.empty();
        }
        return Mono.when(tsIdx, tenantIdx, corrIdx);
    }

    public static Document buildMaintenanceOrdersSchema() {
        Document workshopProps = new Document()
                .append("workshopName", new Document("bsonType", List.of("string", "null")))
                .append("workshopRuc", new Document("bsonType", List.of("string", "null")))
                .append("mechanicInCharge", new Document("bsonType", List.of("string", "null")))
                .append("contactPhone", new Document("bsonType", List.of("string", "null")))
                .append("workshopAddress", new Document("bsonType", List.of("string", "null")))
                .append("estimatedCost", new Document("bsonType", List.of("decimal", "null"))
                        .append("minimum", 0)
                        .append("maximum", 99999999.99))
                .append("finalCost", new Document("bsonType", List.of("decimal", "null"))
                        .append("minimum", 0)
                        .append("maximum", 99999999.99))
                .append("estimatedDeliveryDate", new Document("bsonType", List.of("date", "null")))
                .append("initialDiagnosis", new Document("bsonType", List.of("string", "null")));

        Document properties = new Document()
                .append("orderCode", new Document("bsonType", "string").append("minLength", 1))
                .append("resourceId", new Document("bsonType", "string").append("minLength", 1))
                .append("resourcePlate", new Document("bsonType", "string").append("minLength", 1))
                .append("entityId", new Document("bsonType", "string").append("minLength", 1))
                .append("reportedByDni", new Document("bsonType", List.of("string", "null")))
                .append("incidentId", new Document("bsonType", List.of("string", "null")))
                .append("maintenanceType", new Document("bsonType", "string").append("minLength", 1))
                .append("orderStatus", new Document("bsonType", "string").append("minLength", 1))
                .append("priority", new Document("bsonType", "string").append("minLength", 1))
                .append("failureReason", new Document("bsonType", "string").append("minLength", 1))
                .append("reportedMileage", new Document("bsonType", List.of("int", "long", "null")).append("minimum", 0))
                .append("workshopDetail", new Document("bsonType", List.of("object", "null")).append("properties", workshopProps))
                .append("technicalDischargeReport", new Document("bsonType", List.of("object", "null")))
                .append("remarks", new Document("bsonType", List.of("string", "null")))
                .append("status", new Document("bsonType", "string").append("enum", List.of("A", "I")))
                .append("active", new Document("bsonType", "bool"))
                .append("version", new Document("bsonType", List.of("long", "int", "null")))
                .append("createdAt", new Document("bsonType", "date"))
                .append("updatedAt", new Document("bsonType", "date"));

        return new Document("bsonType", "object")
                .append("required", List.of(
                        "orderCode",
                        "resourceId",
                        "resourcePlate",
                        "entityId",
                        "maintenanceType",
                        "orderStatus",
                        "priority",
                        "failureReason",
                        "status",
                        "active",
                        "createdAt",
                        "updatedAt"))
                .append("properties", properties);
    }

    public static Document buildMaintenanceOrderAuditsSchema() {
        Document properties = new Document()
                .append("timestamp", new Document("bsonType", "date"))
                .append("action", new Document("bsonType", "string").append("minLength", 1))
                .append("orderCode", new Document("bsonType", "string").append("minLength", 1))
                .append("resourceId", new Document("bsonType", "string").append("minLength", 1))
                .append("actor", new Document("bsonType", "string").append("minLength", 1))
                .append("tenant", new Document("bsonType", "string").append("minLength", 1))
                .append("result", new Document("bsonType", "string").append("enum", List.of("SUCCESS", "FAILED")))
                .append("correlationId", new Document("bsonType", "string").append("minLength", 1))
                .append("errorDetails", new Document("bsonType", List.of("string", "null")));

        return new Document("bsonType", "object")
                .append("required", List.of(
                        "timestamp",
                        "action",
                        "orderCode",
                        "resourceId",
                        "actor",
                        "tenant",
                        "result",
                        "correlationId"))
                .append("properties", properties);
    }
}
