package test;

import java.util.List;

import org.easymock.Mock;
import org.easymock.TestSubject;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.junit.Test;
import org.powermock.core.classloader.annotations.PrepareForTest;

@PrepareForTest({HtmlReportBuilder.class, LocalDate.class, DateTime.class})
public class AnonymizationOfCandidateReportMailerTest extends MockingSupport {

    @TestSubject
    private AnonymizationOfCandidateReportMailer subject = new AnonymizationOfCandidateReportMailer();
    @Mock
    private Mailer mailer;
    @Mock
    private CandidateAnonymizer candidateAnonymizer;

    private final static String HEADLINE = "Upozornění na anonymizaci kandidáta";
    private final static String BODY_DATE_WILL_EXPIRE = "U kandidáta %s dojde %s k anonymizaci, protože jeho/její Informovaný nebo Obecný souhlas vyprší.";
    private final static String BODY_NO_DATE = "U kandidáta %s dojde %s k anonymizaci, protože nemá vyplněný Informovaný nebo Obecný souhlas.";
    private final static String NAME = "Tomáš Novotný";
    private final static String MAIL = "hrmail@mail.cz";
    private final static Iterable<String> SEND_TO = Lists.newArrayList(MAIL);
    private final static String DATE_OF_ANONYMIZATION = "7. 9. 2021";
    private final static LocalDate CONSENT_VALID_TO = LocalDate.parse("2021-09-07");
    private final static LocalDate DATE_NOW_TEST = LocalDate.parse("2021-08-31");
    private final static DateTime DATE_CREATED_FOR_OLD_DATE = DateTime.parse("2020-01-01");
    private final static DateTime DATE_CREATED_FOR_NEW_CANDIDATES = DateTime.parse("2021-07-27");


    @Test
    public void sendCandidatesWeekBeforeAnonymizationWithOldDate_returnsExpectedResult() {
        BranchDO branch = mock(BranchDO.class);
        expect(branch.getHrRecipients()).andReturn(SEND_TO);

        CandidateDO candidate = mock(CandidateDO.class);
        expect(candidate.getConsentValidTo()).andReturn(CONSENT_VALID_TO).times(3);
        expect(candidate.getDateCreated()).andReturn(DATE_CREATED_FOR_OLD_DATE).times(1);
        expect(candidate.getFullName()).andReturn(NAME);
        expect(candidate.getBranch()).andReturn(branch);

        String mailBody = (String.format(BODY_DATE_WILL_EXPIRE, NAME, DATE_OF_ANONYMIZATION));
        createMailBody(mailBody);

        mockLocalDateNow(2);
        setUpCandidates(candidate);

        mailer.sendMail(SEND_TO, HEADLINE, mailBody);
        expectLastCall().times(1);

        replayAll();
        subject.reportAnonymization();
        verifyAll();
    }

    @Test
    public void sendNewCandidatesWithoutDate_returnsExpectedResult() {
        BranchDO branch = mock(BranchDO.class);
        expect(branch.getHrRecipients()).andReturn(SEND_TO);

        CandidateDO candidate = mock(CandidateDO.class);
        expect(candidate.getDateCreated()).andReturn(DATE_CREATED_FOR_NEW_CANDIDATES).times(2);
        expect(candidate.getConsentValidTo()).andReturn(null).times(2);
        expect(candidate.isAnonymized()).andReturn(false);
        expect(candidate.getStatus()).andReturn(CandidateStatus.INITIATION);
        expect(candidate.getFullName()).andReturn(NAME);
        expect(candidate.getBranch()).andReturn(branch);

        String mailBody = (String.format(BODY_NO_DATE, NAME, DATE_OF_ANONYMIZATION));
        createMailBody(mailBody);

        mockLocalDateNow(1);
        setUpCandidates(candidate);

        mailer.sendMail(SEND_TO, HEADLINE, mailBody);
        expectLastCall().times(1);

        replayAll();
        subject.reportAnonymization();
        verifyAll();
    }

    @Test(expected = NullPointerException.class)
    public void sendAnonymizationOfCandidateReportNoCandidates_throwsNpe() {
        setUpCandidates(null);
        replayAll();
        subject.reportAnonymization();
        verifyAll();
    }

    private String createMailBody(String mailBody) {
        mockStatic(HtmlReportBuilder.class);
        HtmlReportBuilder builderMock = mock(HtmlReportBuilder.class);
        expect(HtmlReportBuilder.newInstance()).andReturn(builderMock).times(1);
        expect(builderMock.headline(HEADLINE)).andReturn(builderMock);
        expect(builderMock.body(mailBody)).andReturn(builderMock).times(1);
        expect(builderMock.build()).andReturn(mailBody).times(1);
        return mailBody;
    }

    private void setUpCandidates(CandidateDO candidate) {
        List candidates = Lists.newArrayList(candidate);
        expect(candidateAnonymizer.getCandidatesToAnonymize()).andReturn(candidates);
    }

    private void mockLocalDateNow(int times) {
        LocalDate now = DATE_NOW_TEST;
        mockStatic(LocalDate.class);
        expect(LocalDate.now()).andReturn(now).times(times);
    }

}

