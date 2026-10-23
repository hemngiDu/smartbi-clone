package com.smartbi.query;
import org.junit.jupiter.api.Test;import static org.assertj.core.api.Assertions.*;
class NamedParameterSqlTest {
 @Test void replacesNamedParametersButNotQuotedColons(){var parsed=NamedParameterSql.parse("select ':literal', * from sales where amount >= :minimum and region = :region");assertThat(parsed.sql()).isEqualTo("select ':literal', * from sales where amount >= ? and region = ?");assertThat(parsed.names()).containsExactly("minimum","region");}
}
