package todobackend.ratpack;

import com.google.common.reflect.TypeToken;
import ratpack.func.Function;
import ratpack.guice.Guice;
import ratpack.hikari.HikariModule;
import ratpack.http.MutableHeaders;
import ratpack.http.Response;
import ratpack.jackson.Jackson;
import ratpack.jackson.JsonRender;
import ratpack.registry.Registry;
import ratpack.server.RatpackServer;

import java.util.Map;
import java.util.stream.Collectors;

public class TodoApp {
  public static void main(String[] args) throws Exception {
    RatpackServer.start(ratpackServerSpec -> ratpackServerSpec
      .registry(Guice.registry(bindingsSpec -> bindingsSpec
        .module(HikariModule.class, hikariConfig -> {
          hikariConfig.addDataSourceProperty("URL", "jdbc:h2:mem:todo;INIT=RUNSCRIPT FROM 'classpath:/create.sql'");
          hikariConfig.setDataSourceClassName("org.h2.jdbcx.JdbcDataSource");
        })
        .bind(TodoRepository.class)
      ))
      .handlers(chain -> chain
        .prefix("todo", todoChain -> todoChain
          .all(ctx -> {
            MutableHeaders headers = ctx.getResponse().getHeaders();
            headers.set("Access-Control-Allow-Origin", "*");
            headers.set("Access-Control-Allow-Headers", "x-requested-with, origin, content-type, accept");
            ctx.next(Registry.single(String.class, "https://" + ctx.getRequest().getHeaders().get("Host") + "/todo"));
          })
          .path(ctx -> {

            TodoRepository repo = ctx.get(TodoRepository.class);
            String base = ctx.get(String.class);

            ctx.byMethod(byMethodSpec -> byMethodSpec
              .options(() -> {
                ctx.getResponse().getHeaders().set("Access-Control-Allow-Methods", "OPTIONS, GET, POST, DELETE");
                ctx.getResponse().send();
              })
              .get(() ->
                repo.getAll()
                  .map(todos -> todos.stream()
                    .map(todo -> todo.baseUrl(base))
                    .collect(Collectors.toList()))
                  .map(Jackson::json)
                  .then(ctx::render)
              )
              .post(() ->
                ctx.parse(Jackson.fromJson(Todo.class))
                  .flatMap(repo::add)
                  .map(todo -> todo.baseUrl(base))
                  .map(Jackson::json)
                  .then(ctx::render)
              )
              .delete(() -> repo.deleteAll().then(() -> ctx.getResponse().send()))
            );
          })
          .path(":id", ctx -> {

            TodoRepository repo = ctx.get(TodoRepository.class);
            String base = ctx.get(String.class);
            Long todoId = Long.parseLong(ctx.getPathTokens().get("id"));

            Function<Todo, Todo> hostUpdater = todo -> todo.baseUrl(base);
            Function<Todo, JsonRender> toJson = hostUpdater.andThen(Jackson::json);

            Response response = ctx.getResponse();

            ctx.byMethod(byMethodSpec -> byMethodSpec
              .options(() -> {
                response.getHeaders().set("Access-Control-Allow-Methods", "OPTIONS, GET, PATCH, DELETE");
                response.send();
              })
              .get(() -> repo.getById(todoId).map(toJson).then(ctx::render))
              .patch(() -> ctx
                .parse(Jackson.fromJson(new TypeToken<Map<String, Object>>(){}))
                .flatMap(m -> repo.update(todoId, m))
                .map(t -> t.baseUrl(base))
                .map(toJson)
                .then(ctx::render)
              )
              .delete(() -> repo.delete(todoId).then(response::send))
            );
          })
        )
      ));
  }
}
