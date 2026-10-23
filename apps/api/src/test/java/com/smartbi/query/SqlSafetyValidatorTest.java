package com.smartbi.query;
import org.junit.jupiter.api.Test;import static org.assertj.core.api.Assertions.*;
class SqlSafetyValidatorTest {
 private final SqlSafetyValidator validator=new SqlSafetyValidator();
 @Test void acceptsSelectAndWith(){assertThatCode(()->validator.validate("select * from sales where amount > :minimum")).doesNotThrowAnyException();assertThatCode(()->validator.validate("with totals as (select sum(amount) value from sales) select * from totals")).doesNotThrowAnyException();}
 @Test void rejectsWritesCommentsAndStackedStatements(){assertThatThrownBy(()->validator.validate("delete from sales")).isInstanceOf(QueryException.class);assertThatThrownBy(()->validator.validate("select 1; drop table sales")).isInstanceOf(QueryException.class);assertThatThrownBy(()->validator.validate("select * from sales -- bypass")).isInstanceOf(QueryException.class);}
}
