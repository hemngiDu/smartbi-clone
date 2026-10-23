package com.smartbi.query;
import org.junit.jupiter.api.Test;import static org.assertj.core.api.Assertions.*;
class SqlSafetyValidatorTest{
 private final SqlSafetyValidator validator=new SqlSafetyValidator();
 @Test void acceptsSelectCteCastsAndKeywordsInsideLiterals(){assertThatCode(()->validator.validate("select 'delete; --' value, 1::numeric /* harmless */")).doesNotThrowAnyException();assertThatCode(()->validator.validate("with totals as (select sum(amount) value from sales) select * from totals")).doesNotThrowAnyException();}
 @Test void rejectsWritesInCtesSelectIntoStackingAndSideEffects(){for(String sql:java.util.List.of("delete from sales","with changed as (delete from sales returning *) select * from changed","select * into backup from sales","select pg_sleep(1)","select 1; drop table sales"))assertThatThrownBy(()->validator.validate(sql)).as(sql).isInstanceOf(QueryException.class);}
}
