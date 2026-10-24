package com.smartbi.query;
import org.junit.jupiter.api.Test;import static org.assertj.core.api.Assertions.*;
class NamedParameterSqlTest{
 @Test void lexesPostgresSyntaxWithoutTurningContentIntoParameters(){String sql="select value::numeric, ':literal', E'it''s :x', \"odd:name\", $$:dollar$$, $tag$:tagged$tag$ /* :block */ -- :line\nfrom sales where amount>=:minimum and id=:id";var parsed=NamedParameterSql.parse(sql);assertThat(parsed.names()).containsExactly("minimum","id");assertThat(parsed.sql()).endsWith("amount>=? and id=?");assertThat(parsed.sql()).contains("value::numeric","$$:dollar$$","/* :block */","-- :line");}
 @Test void supportsRepeatedNamesAndRejectsUnterminatedConstructs(){assertThat(NamedParameterSql.parse("select :x + :x").names()).containsExactly("x","x");assertThatThrownBy(()->NamedParameterSql.parse("select $tag$broken")).isInstanceOf(QueryException.class);assertThatThrownBy(()->NamedParameterSql.parse("select /* broken")).isInstanceOf(QueryException.class);}
}
