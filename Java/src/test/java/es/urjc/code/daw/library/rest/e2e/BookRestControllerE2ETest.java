package es.urjc.code.daw.library.rest.e2e;

import org.json.JSONException;
import org.json.JSONObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.fail;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;

import es.urjc.code.daw.library.book.Book;
import es.urjc.code.daw.library.book.BookService;
import static es.urjc.code.daw.library.rest.TestUtils.*;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import static io.restassured.RestAssured.*;
import static io.restassured.path.json.JsonPath.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BookRestControllerE2ETest {

    @LocalServerPort
    int port;

    @Autowired
    BookService bookService;

    @BeforeEach
    public void setup() {
        RestAssured.port = port;
        RestAssured.useRelaxedHTTPSValidation();
        RestAssured.baseURI = "https://localhost:" + port;
    }

    /**
     * Get all books tests: [GET]/api/books/
     */

    @Test
    @DisplayName("[E2E] Not logged user can get all the books")
    void givenNotLoggedUserWhenGetAllBooksThenReturnBooks() {
        List<Book> savedBooks = Stream.of(bookService.save(new Book("Title 1", "Description 1")),
                bookService.save(new Book("Title 2", "Description 2")),
                bookService.save(new Book("Title 3", "Description 3"))).collect(Collectors.toList());

        given().when().get(BOOKS_ENDPOINT).then().statusCode(HttpStatus.OK.value())
                .body("id",
                    hasItems(savedBooks.stream()
                        .map((book) -> book.getId().intValue())
                        .collect(Collectors.toList())
                        .toArray()))
                .body("title", 
                    hasItems(savedBooks.stream()
                        .map(Book::getTitle)
                        .collect(Collectors.toList())
                        .toArray()))
                .body("description",
                    hasItems(savedBooks.stream()
                    .map(Book::getDescription)
                    .collect(Collectors.toList())
                    .toArray()));
    }

    /**
     * Add book tests: [POST]/api/books/
     * @throws JSONException
     */

    @Test
    @DisplayName("[E2E] Not logged user cannot add a book")
    void givenNotLoggedUserWhenAddBookThenUnauthorized() throws JSONException {
        JSONObject jsonObj = new JSONObject()
                                .put("title","Title 1")
                                .put("description","Description 1");

        Response response = given()
                                .contentType(ContentType.JSON)
                                .body(jsonObj.toString())
                                .post(BOOKS_ENDPOINT)
                                .andReturn();
        
        assertThat(response.getBody().asString(), containsString(UNAUTHORIZED_STRING));
    }

    @Test
    @DisplayName("[E2E] Logged user can add a book successfully")
    void givenLoggedUserWhenAddBookThenCreatesSuccessfully() throws JSONException {
        Book book = new Book("Title 1", "Description 1");

        JSONObject jsonObj = new JSONObject()
                                .put("title", book.getTitle())
                                .put("description", book.getDescription());

        Response response = given()
                                .auth()
                                    .basic(USER_USERNAME, USER_PASSWORD)
                                .contentType(ContentType.JSON)
                                .body(jsonObj.toString())
                                .post(BOOKS_ENDPOINT)
                                .andReturn();
        
        Integer id = from(response.getBody().asString()).get("id");

        Optional<Book> bookFromDb = bookService.findOne(id.longValue());

        if(!bookFromDb.isPresent()) fail();

        assertThat(response.statusCode(), is(HttpStatus.CREATED.value()));
        assertThat(bookFromDb.get().getTitle(), is(book.getTitle()));
        assertThat(bookFromDb.get().getDescription(), is(book.getDescription()));
        
    }

    @Test
    @DisplayName("[E2E] Logged admin can add a book successfully (it has also USER role)")
    void givenLoggedAdminWhenAddBookThenCreatesSuccessfully() throws JSONException {
        Book book = new Book("Title 1", "Description 1");

        JSONObject jsonObj = new JSONObject()
                                .put("title", book.getTitle())
                                .put("description", book.getDescription());

        Response response = given()
                                .auth()
                                    .basic("admin", "pass")
                                .contentType(ContentType.JSON)
                                .body(jsonObj.toString())
                                .post(BOOKS_ENDPOINT)
                                .andReturn();
        
        Integer id = from(response.getBody().asString()).get("id");

        Optional<Book> bookFromDb = bookService.findOne(id.longValue());

        if(!bookFromDb.isPresent()) fail();

        assertThat(response.statusCode(), is(HttpStatus.CREATED.value()));
        assertThat(bookFromDb.get().getTitle(), is(book.getTitle()));
        assertThat(bookFromDb.get().getDescription(), is(book.getDescription()));
    }

    /**
     * Delete book tests: [DELETE]/api/books/{id}
     */

    @Test
    @DisplayName("[E2E] Not logged user cannot delete a book")
    void givenNotLoggedUserWhenDeleteBookThenUnauthorized() {
        Response response = given()
                                .pathParam("id", 1)
                                .delete(BOOKS_ENDPOINT + "{id}")
                                .andReturn();
        
        assertThat(response.statusCode(), is(HttpStatus.UNAUTHORIZED.value()));
        assertThat(response.getBody().asString(), containsString(UNAUTHORIZED_STRING));
    }

    @Test
    @DisplayName("[E2E] Logged user (not admin) cannot delete a book")
    void givenLoggedUserWhenDeleteBookThenForbidden() {
        Response response = given()
                                .auth()
                                    .basic(USER_USERNAME, USER_PASSWORD)
                                .pathParam("id", 1)
                                .delete(BOOKS_ENDPOINT + "{id}")
                                .andReturn();
        
        assertThat(response.statusCode(), is(HttpStatus.FORBIDDEN.value()));
        assertThat(response.getBody().asString(), containsString(FORBIDDEN_STRING));
    }

    @Test
    @DisplayName("[E2E] Logged admin can delete a book")
    void givenLoggedAdminWhenDeleteBookThenDeletesSuccesfully() {
        Book book = bookService.save(new Book("Title 1", "Description 1"));

        Response response = given()
                                .auth()
                                    .basic(ADMIN_USERNAME, ADMIN_PASSWORD)
                                .pathParam("id", book.getId())
                                .delete(BOOKS_ENDPOINT + "{id}")
                                .andReturn();

        Optional<Book> bookFromDb = bookService.findOne(book.getId());
        
        assertThat(response.statusCode(), is(HttpStatus.OK.value()));
        assertThat(bookFromDb.isPresent(), is(false));
    }

}
