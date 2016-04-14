package todobackend.ratpack;

import com.google.common.reflect.TypeToken;
import ratpack.func.Function;
import ratpack.handling.Context;
import ratpack.handling.InjectionHandler;
import ratpack.http.Response;
import ratpack.jackson.Jackson;
import ratpack.jackson.JsonRender;

import java.util.Map;

public class TodoIdHandler extends InjectionHandler {
  public void handle(Context ctx, TodoRepository repo, String base) throws Exception {
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
        .parse(Jackson.fromJson(new TypeToken<Map<String, Object>>() {
        }))
        .flatMap(m -> repo.update(todoId, m))
        .map(t -> t.baseUrl(base))
        .map(toJson)
        .then(ctx::render)
      )
      .delete(() -> repo.delete(todoId).then(response::send))
    );
  }
}
