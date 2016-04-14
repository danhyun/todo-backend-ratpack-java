package todobackend.ratpack;

import ratpack.guice.Guice;
import ratpack.hikari.HikariModule;
import ratpack.server.RatpackServer;

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
          .all(new CORSHandler())
          .path(new TodoBaseHandler())
          .path(":id", new TodoIdHandler())
        )
      ));
  }
}
