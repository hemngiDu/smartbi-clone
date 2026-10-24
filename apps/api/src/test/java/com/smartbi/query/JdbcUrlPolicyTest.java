package com.smartbi.query;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
class JdbcUrlPolicyTest {
 private final JdbcUrlPolicy policy=new JdbcUrlPolicy("analytics-db,db.example.test,2001:db8::10","5432,6432");
 @Test void acceptsCanonicalSingleEndpoint(){assertThatCode(()->policy.validatePostgreSql("jdbc:postgresql://analytics-db:5432/analytics")).doesNotThrowAnyException();assertThatCode(()->policy.validatePostgreSql("jdbc:postgresql://[2001:db8::10]:6432/warehouse")).doesNotThrowAnyException();}
 @Test void rejectsMultiHostUserInfoParametersAndParserBypasses(){for(String url:java.util.List.of("jdbc:postgresql://analytics-db:5432,evil:5432/analytics","jdbc:postgresql://analytics-db,evil/analytics","jdbc:postgresql://user@analytics-db/analytics","jdbc:postgresql://analytics-db/analytics?socketFactory=evil.Factory","jdbc:postgresql://analytics-db/analytics#fragment","jdbc:postgresql:analytics","jdbc:postgresql://analytics-db:5432/analytics/extra","jdbc:postgresql://analytics-db%2Fevil/analytics","jdbc:postgresql://169.254.169.254/latest"))assertThatThrownBy(()->policy.validatePostgreSql(url)).as(url).isInstanceOf(QueryException.class);}
}
