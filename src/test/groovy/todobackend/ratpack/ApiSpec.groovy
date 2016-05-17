package todobackend.ratpack

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import ratpack.test.MainClassApplicationUnderTest
import ratpack.test.http.TestHttpClient
import spock.lang.AutoCleanup
import spock.lang.Specification

public class ApiSpec extends Specification {

  @AutoCleanup
  MainClassApplicationUnderTest aut = new MainClassApplicationUnderTest(TodoApp)

  @Delegate
  TestHttpClient client = aut.httpClient

  def 'can create and update todos'() {
    given:
    jsonBody = [title: 'create app']

    when:
    post('todo')

    then:
    def json = new JsonSlurper().parse(response.body.bytes)
    json.id
    !json.completed
    json.title == 'create app'

    when:
    jsonBody = [completed: true]

    patch("todo/${json.id}")

    then:
    def updated = new JsonSlurper().parse(response.body.bytes)
    updated.id
    updated.completed
    updated.title == 'create app'
  }

  private void setJsonBody(Map<String, Object> map) {
    requestSpec { spec ->
      spec.body
        .type('application/json')
        .text(JsonOutput.toJson(map))
    }
  }
}
