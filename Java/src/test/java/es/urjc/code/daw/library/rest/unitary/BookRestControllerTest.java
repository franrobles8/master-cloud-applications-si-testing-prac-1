package es.urjc.code.daw.library.rest.unitary;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import es.urjc.code.daw.library.book.Book;
import es.urjc.code.daw.library.book.BookService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;

@SpringBootTest
@AutoConfigureMockMvc
public class BookRestControllerTest {

    private static final int UNAUTHORIZED_STATUS = 401;
    private static final int FORBIDDEN_STATUS = 403;
    private static final String BOOKS_ENDPOINT = "/api/books/";
    
    @Autowired
    MockMvc mockMvc;

    @MockBean
    BookService bookService;
    
    @Test
    @DisplayName("Not logged user can get all the books")
    public void givenNotLoggedUserWhenGetAllBooksThenReturnBooks() throws Exception {
        List<Book> books = Arrays.asList(new Book("Title 1", "Description 1"), new Book("Title 2", "Description 2"));

        when(bookService.findAll()).thenReturn(books);

        mockMvc.perform(
            get(BOOKS_ENDPOINT)
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].title").value(books.get(0).getTitle()))
            .andExpect(jsonPath("$[0].description").value(books.get(0).getDescription()))
            .andExpect(jsonPath("$[1].title").value(books.get(1).getTitle()))
            .andExpect(jsonPath("$[1].description").value(books.get(1).getDescription()));
    }

    @Test
    @DisplayName("Not logged user cannot create a new book")
    public void givenNotLoggedUserWhenCreateBookThenUnauthorized() throws Exception {
        Book book = new Book("Title 1", "Description 1");

        when(bookService.save(book)).thenReturn(book);

        mockMvc.perform(
            post(BOOKS_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(book)))
            .andExpect(status().is(UNAUTHORIZED_STATUS))
            .andExpect(status().reason("Unauthorized"));
    }

    @Test
    @DisplayName("Logged user can create a new book")
    @WithMockUser(username = "user", password = "pass", roles = "USER")
    public void givenLoggedUserWhenCreateBookThenCreatesBookSuccessfully() throws Exception {
        Book book = new Book("Title 1", "Description 1");

        when(bookService.save(book)).thenReturn(book);

        mockMvc.perform(
            post(BOOKS_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(book)))
            .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Logged admin can create a new book (it also has USER role)")
    @WithMockUser(username = "admin", password = "pass", roles = {"USER", "ADMIN"})
    public void givenLoggedAdminWhenCreateBookThenCreatesBookSuccessfully() throws Exception {
        Book book = new Book("Title 1", "Description 1");

        when(bookService.save(book)).thenReturn(book);

        mockMvc.perform(
            post(BOOKS_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(book)))
            .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Not logged user cannot delete a book")
    public void givenNotLoggedUserWhenDeleteBookThenUnauthorized() throws Exception {
        mockMvc.perform(
            delete(BOOKS_ENDPOINT + "1"))
            .andExpect(status().is(UNAUTHORIZED_STATUS))
            .andExpect(status().reason("Unauthorized"));
    }

    @Test
    @DisplayName("Logged user (not admin) cannot delete a book")
    @WithMockUser(username = "user", password = "pass", roles = "USER")
    public void givenLoggedUserWhenDeleteBookThenForbidden() throws Exception {
        mockMvc.perform(
            delete(BOOKS_ENDPOINT + "1"))
            .andExpect(status().is(FORBIDDEN_STATUS))
            .andExpect(status().reason("Forbidden"));
    }

    @Test
    @DisplayName("Logged admin can delete a book")
    @WithMockUser(username = "admin", password = "pass", roles = {"USER", "ADMIN"})
    public void givenLoggedAdminWhenDeleteBookThenDeletesSuccessfully() throws Exception {
        mockMvc.perform(
            delete(BOOKS_ENDPOINT + "1"))
            .andExpect(status().isOk());
    }

    private static String asJsonString(final Object obj) {
        try {
            return new ObjectMapper().writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


}