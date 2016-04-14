package todobackend.ratpack;

import com.google.common.collect.Maps;
import jooq.tables.records.TodoRecord;
import org.jooq.*;
import org.jooq.impl.DSL;
import org.jooq.impl.DefaultConfiguration;
import ratpack.exec.Blocking;
import ratpack.exec.Operation;
import ratpack.exec.Promise;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static jooq.tables.Todo.TODO;

@Singleton
public class TodoRepository {

  final DSLContext context;

  @Inject
  public TodoRepository(DataSource ds) {
    this.context = DSL.using(new DefaultConfiguration().derive(ds));
  }

  public Promise<List<Todo>> getAll() {
    SelectJoinStep all = context.select().from(TODO);
    return Blocking.get(() -> all.fetchInto(Todo.class));
  }

  public Promise<Todo> getById(Long id) {
    SelectConditionStep where = context.select().from(TODO).where(TODO.ID.equal(id));
    return Blocking.get(() -> where.fetchOne().into(Todo.class));
  }

  public Promise<Todo> add(Todo todo) {
    TodoRecord todoRecord = context.newRecord(TODO, todo);
    return Blocking.get(() -> {
      todoRecord.store();
      return todoRecord.getId();
    }).flatMap(this::getById);
  }

  class FieldEntry {
    final Field field;
    final String key;
    final Object value;

    FieldEntry(Field field, String key, Object value) {
      this.field = field;
      this.key = key;
      this.value = value;
    }
  }

  private FieldEntry getField(Map.Entry<String, Object> entry) {
    return Stream.of(TODO.fields())
      .filter(f -> f.getName().equalsIgnoreCase(entry.getKey()))
      .findAny()
      .map(f ->
        new FieldEntry(f, entry.getKey(), entry.getValue())
      ).orElse(null);
  }

  public Promise<Todo> update(Long id, Map<String, Object> map) {

    Map<Field<Object>, Object> fields = Maps.newHashMap();
    map.entrySet().stream()
      .map(this::getField)
      .filter(Objects::nonNull)
      .forEach(f -> fields.put(f.field, f.value));

    UpdateConditionStep<TodoRecord> update = context.update(TODO)
      .set(fields)
      .where(TODO.ID.eq(id));

    return Blocking.get(update::execute).flatMap(i -> getById(id));
  }

  public Operation delete(Long id) {
    DeleteConditionStep<TodoRecord> deleteWhereId = context.deleteFrom(TODO).where(TODO.ID.equal(id));
    return Blocking.op(deleteWhereId::execute);
  }

  public Operation deleteAll() {
    DeleteWhereStep<TodoRecord> delete = context.deleteFrom(TODO);
    return Blocking.op(delete::execute);
  }
}
