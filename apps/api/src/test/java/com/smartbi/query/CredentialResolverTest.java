package com.smartbi.query;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
class CredentialResolverTest {
 @Test void deniesEnvironmentVariablesOutsideExplicitAllowList(){var resolver=new CredentialResolver("APP_DB_PASSWORD");assertThatThrownBy(()->resolver.resolve("env://PATH")).isInstanceOf(QueryException.class).hasMessageNotContaining(System.getenv("PATH"));}
}
