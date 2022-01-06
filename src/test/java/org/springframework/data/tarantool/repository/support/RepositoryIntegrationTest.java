package org.springframework.data.tarantool.repository.support;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.tarantool.BaseIntegrationTest;
import org.springframework.data.tarantool.entities.Book;
import org.springframework.data.tarantool.entities.TestEntityWithDoubleField;
import org.springframework.data.tarantool.entities.TestEntityWithFloatField;
import org.springframework.data.tarantool.entities.TestObject;
import org.springframework.data.tarantool.repository.BookRepository;
import org.springframework.data.tarantool.repository.TestDoubleRepository;
import org.springframework.data.tarantool.repository.TestFloatRepository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Alexey Kuzin
 */
@Tag("integration")
class RepositoryIntegrationTest extends BaseIntegrationTest {
    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private TestDoubleRepository testDoubleRepository;

    @Autowired
    private TestFloatRepository testFloatRepository;

    @BeforeEach
    public void setUp() {
        Book donQuixote = Book.builder()
                .id(1).uniqueKey("a1").name("Don Quixote").author("Miguel de Cervantes").year(1605).build();
        Book theGreatGatsby = Book.builder()
                .id(2).uniqueKey("a2").name("The Great Gatsby").author("F. Scott Fitzgerald").year(1925).build();
        Book warAndPeace = Book.builder()
                .id(3).uniqueKey("a3").name("War and Peace").author("Leo Tolstoy").year(1869).build();
        bookRepository.save(donQuixote);
        bookRepository.save(theGreatGatsby);
        bookRepository.save(warAndPeace);
    }

    @AfterEach
    public void tearDown() {
        bookRepository.deleteAll();
        testDoubleRepository.deleteAll();
        testFloatRepository.deleteAll();
    }

    @Test
    public void findOne_shouldReturnNullForNonExistingKey() {
        Optional<Book> one = bookRepository.findById(123);

        assertThat(one).isNotPresent();
    }

    @Test
    public void testSave() {
        Book book = Book.builder().id(4).name("Tales").uniqueKey("udf65").author("Grimm Brothers").year(1569).build();
        Book newBook = bookRepository.save(book);
        assertThat(newBook).isEqualTo(book);
    }

    @Test
    public void testExists() {
        Book book = Book.builder()
                .id(5).name("Lady of the Lake").uniqueKey("udf66").author("Anjey Sapkovski").year(1986).build();
        Book newBook = bookRepository.save(book);
        assertThat(bookRepository.existsById(5)).isTrue();
    }

    @Test
    public void testDelete() {
        Book book = Book.builder()
                .id(6).name("The Wizard of the Emerald Town")
                .uniqueKey("udf67").author("Leonid Volkov").year(1978).build();
        Book newBook = bookRepository.save(book);
        bookRepository.delete(newBook);
        assertThat(bookRepository.existsById(6)).isFalse();
    }

    @Test
    public void test_deleteAll() {
        List<Book> books = (List<Book>) bookRepository.findAll();
        assertEquals(3, books.size());
        bookRepository.deleteAll();
        books = (List<Book>) bookRepository.findAll();
        assertEquals(0, books.size());
    }

    @Test
    public void testFindById() {
        Optional<Book> book = bookRepository.findById(3);
        assertThat(book).hasValueSatisfying(actual -> {
            assertThat(actual.getName()).isEqualTo("War and Peace");
            assertThat(actual.getAuthor()).isEqualTo("Leo Tolstoy");
        });
    }

    @Test
    public void testFindAll() {
        List<Book> books = (List<Book>) bookRepository.findAll();
        assertThat(books.size()).isGreaterThanOrEqualTo(3);
    }

    @Test
    public void testFindByYear() {
        List<Book> books = bookRepository.findByYearGreaterThenProxy(1000);
        assertThat(books.size()).isGreaterThan(0);
    }

    @Test
    public void testFindByEntity() {
        Optional<Book> book = bookRepository.findById(3);
        assertThat(book.isPresent()).isTrue();
        List<Book> books = bookRepository.findByBook(book.get());
        assertThat(books.size()).isEqualTo(1);
        assertThat(books.get(0).getName()).isEqualTo("War and Peace");
    }

    @Test
    public void testComplexQueryWithMapReduce() {
        List<Book> books = bookRepository.getListByName(Arrays.asList("Don Quixote", "War and Peace"));
        assertThat(books.size()).isEqualTo(2);
        assertThat(books.get(0).getAuthor()).isEqualTo("Miguel de Cervantes");
        assertThat(books.get(0).getYear()).isEqualTo(1605);
    }

    @Test
    public void testUpdateYear() {
        Book book = Book.builder()
                .id(777).name("Red and Black")
                .uniqueKey("udf99").author("Stendal").year(1999).build();
        Book newBook = bookRepository.save(book);
        bookRepository.updateYear(777, 2000);
        Optional<Book> one = bookRepository.findById(777);
        assertTrue(one.isPresent());
        Book bookFromRepository = one.get();
        assertThat(bookFromRepository.getYear()).isEqualTo(2000);
    }

    @Test
    public void testBatchSave() {
        Book book1 = Book.builder()
                .id(888).name("Tamerlan")
                .uniqueKey("udf888").author("Viktor Yan").year(1979).build();
        Book book2 = Book.builder()
                .id(888).name("Bratya Karamazovy")
                .uniqueKey("udf999").author("Fedor Dostoevsky").year(1888).build();
        List<Book> savedBooks = bookRepository.batchSave(Arrays.asList(book1, book2));
        assertTrue(savedBooks.size() > 0);
    }

    @Test
    public void should_testCustomConverter_returnObjectWithDouble_ifCustomConverterHasBeenAdded() {
        //given
        double testField = 1D;

        //when
        TestEntityWithDoubleField saved = testDoubleRepository.save(new TestEntityWithDoubleField(1, testField));

        //then
        assertEquals(testField, saved.getTest());
    }

    @Test
    public void should_test_returnObjectWithFloat_ifFirstInStackConvertersIsDouble() {
        //given
        float testField = 1f;

        //when
        TestEntityWithFloatField savedEntity = testFloatRepository.save(new TestEntityWithFloatField(1, testField));

        //then
        assertEquals(testField, savedEntity.getTest());
    }

    @Test
    public void should_test_returnIntegerFromRepository() {
        //given
        Integer expected = 12345;

        //when
        Integer actual = testDoubleRepository.getInteger();

        //then
        assertEquals(expected, actual);
    }

    @Test
    public void should_test_returnStringFromRepository() {
        //given
        String expected = "test string";

        //when
        String actual = testDoubleRepository.getString();

        //then
        assertEquals(expected, actual);
    }

    @Test
    public void should_test_returnNonEntityObjectFromRepository() {
        //given
        TestObject expected = new TestObject("testString", 4);

        //when
        TestObject actual = testDoubleRepository.getNonEntityObject();

        //then
        assertEquals(expected, actual);
    }

    @Test
    public void should_test_returnNonEntityObjectListFromRepository() {
        //given
        final List<Object> expected = new ArrayList<>();
        TestObject expectedItem = new TestObject("testString", 4);
        TestObject expectedItem2 = new TestObject("testString2", 10);
        expected.add(expectedItem);
        expected.add(expectedItem2);


        //when
        List<TestObject> actual = testDoubleRepository.getNonEntityObjectList();

        //then
        assertEquals(expected, actual);
    }
}
