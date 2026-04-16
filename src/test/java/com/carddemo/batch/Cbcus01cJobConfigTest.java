package com.carddemo.batch;

import com.carddemo.model.CustomerData;
import com.carddemo.service.Cbcus01cService;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link Cbcus01cJobConfig}.
 *
 * <p>Tests cover:
 * <ul>
 *   <li>Job bean is created with the correct name.</li>
 *   <li>Reader bean is a non-null {@link JpaPagingItemReader}.</li>
 *   <li>Processor delegates to {@link Cbcus01cService}.</li>
 *   <li>Writer bean is a non-null {@link JpaItemWriter}.</li>
 *   <li>Processor returns {@code null} for a null input (skip logic).</li>
 *   <li>Processor returns processed record for a valid input.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class Cbcus01cJobConfigTest {

    @Mock
    private EntityManagerFactory entityManagerFactory;

    @Mock
    private JobRepository jobRepository;

    @Mock
    private PlatformTransactionManager transactionManager;

    @Mock
    private Cbcus01cService cbcus01cService;

    @Mock
    private Step cbcus01cStep;

    private Cbcus01cJobConfig jobConfig;

    @BeforeEach
    void setUp() {
        jobConfig = new Cbcus01cJobConfig(entityManagerFactory, cbcus01cService);
        // Simulate @Value injection that Spring would perform at runtime
        ReflectionTestUtils.setField(jobConfig, "chunkSize", 100);
        ReflectionTestUtils.setField(jobConfig, "skipLimit", 10);
    }

    // -------------------------------------------------------------------------
    // Job bean
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("cbcus01cJob bean is created and has the correct name")
    void cbcus01cJob_beanCreated_withCorrectName() {
        Job job = jobConfig.cbcus01cJob(jobRepository, cbcus01cStep);

        assertThat(job).isNotNull();
        assertThat(job.getName()).isEqualTo("cbcus01cJob");
    }

    // -------------------------------------------------------------------------
    // Reader bean
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("cbcus01cItemReader bean is a non-null JpaPagingItemReader")
    void cbcus01cItemReader_beanCreated_notNull() {
        JpaPagingItemReader<CustomerData> reader = jobConfig.cbcus01cItemReader();

        assertThat(reader).isNotNull();
    }

    @Test
    @DisplayName("cbcus01cItemReader is named 'cbcus01cItemReader'")
    void cbcus01cItemReader_hasCorrectName() {
        JpaPagingItemReader<CustomerData> reader = jobConfig.cbcus01cItemReader();

        // getName() returns the logical name set on the reader builder
        assertThat(reader.getName()).isEqualTo("cbcus01cItemReader");
    }

    // -------------------------------------------------------------------------
    // Processor bean
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("cbcus01cItemProcessor bean is non-null")
    void cbcus01cItemProcessor_beanCreated_notNull() {
        ItemProcessor<CustomerData, CustomerData> processor = jobConfig.cbcus01cItemProcessor();

        assertThat(processor).isNotNull();
    }

    @Test
    @DisplayName("Processor delegates to Cbcus01cService.process() and returns its result")
    void cbcus01cItemProcessor_delegatesToService_returnsServiceResult() throws Exception {
        CustomerData input = buildSampleCustomer(1001L);
        CustomerData expected = buildSampleCustomer(1001L);
        expected.setProcessedFlag("Y");

        when(cbcus01cService.process(input)).thenReturn(expected);

        ItemProcessor<CustomerData, CustomerData> processor = jobConfig.cbcus01cItemProcessor();
        CustomerData result = processor.process(input);

        assertThat(result).isEqualTo(expected);
        verify(cbcus01cService, times(1)).process(input);
    }

    @Test
    @DisplayName("Processor returns null when service returns null (skip invalid record)")
    void cbcus01cItemProcessor_returnsNull_whenServiceReturnsNull() throws Exception {
        CustomerData invalidInput = buildSampleCustomer(-1L);
        when(cbcus01cService.process(invalidInput)).thenReturn(null);

        ItemProcessor<CustomerData, CustomerData> processor = jobConfig.cbcus01cItemProcessor();
        CustomerData result = processor.process(invalidInput);

        assertThat(result).isNull();
        verify(cbcus01cService, times(1)).process(invalidInput);
    }

    @Test
    @DisplayName("Processor passes null input to service (service handles null guard)")
    void cbcus01cItemProcessor_handlesNullInput() throws Exception {
        when(cbcus01cService.process(null)).thenReturn(null);

        ItemProcessor<CustomerData, CustomerData> processor = jobConfig.cbcus01cItemProcessor();
        CustomerData result = processor.process(null);

        assertThat(result).isNull();
    }

    // -------------------------------------------------------------------------
    // Writer bean
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("cbcus01cItemWriter bean is a non-null ItemWriter")
    void cbcus01cItemWriter_beanCreated_notNull() {
        ItemWriter<CustomerData> writer = jobConfig.cbcus01cItemWriter();

        assertThat(writer).isNotNull();
        assertThat(writer).isInstanceOf(JpaItemWriter.class);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static CustomerData buildSampleCustomer(long custId) {
        return CustomerData.builder()
                .custId(custId)
                .custFirstName("John")
                .custMiddleName("M")
                .custLastName("Doe")
                .custAddrLine1("123 Main St")
                .custAddrLine2("")
                .custAddrLine3("")
                .custAddrStateCd("TX")
                .custAddrCountryCd("USA")
                .custAddrZip("78701")
                .custPhoneNum1("512-555-0100")
                .custPhoneNum2("")
                .custSsn(123456789L)
                .custGovtIssuedId("DL-TX-987654")
                .custDobYyyyMmDd("1985-04-15")
                .custEftAccountId("EFT0000001")
                .custPriCardHolderInd("Y")
                .custFicoCreditScore(720)
                .processedFlag("N")
                .build();
    }
}
