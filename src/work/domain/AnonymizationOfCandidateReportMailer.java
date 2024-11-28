package work.domain;

@Service
class AnonymizationOfCandidateReportMailer {

    @Autowired
    private lateinit var mailer: Mailer

    @Autowired
    private lateinit var candidateAnonymizer: CandidateAnonymizer

    companion object {
        private const val SUBJECT = "Upozornění na anonymizaci kandidáta"
        private const val BODY_DATE_WILL_EXPIRE = "U kandidáta %s dojde %s k anonymizaci, protože jeho/její Informovaný nebo Obecný souhlas vyprší."
        private const val BODY_NO_DATE = "U kandidáta %s dojde %s k anonymizaci, protože nemá vyplněný Informovaný nebo Obecný souhlas."
        private const val DATE_PATTERN = "d. M. y"
        private const val DAYS_UNTIL_ANONYMIZATION_WITH_OLD_DATE = 7
        private const val WEEKS_TOLERANCE = 5
        private const val WEEKS_SINCE_CREATION_TO_ANONYMIZATION = 6

        @JvmStatic
        private val log = LoggerFactory.getLogger(javaClass.enclosingClass)
    }

    @Transactional
    fun reportAnonymization() {
        val candidates = candidateAnonymizer.candidatesToAnonymize
        val candidatesWeekBeforeAnonymizationWithOldDate = filterCandidatesWeekBeforeAnonymizationWithOldDate(candidates)
        val newCandidatesWeekBeforeAnonymizationWithoutDate = filterOutNewCandidatesWithoutDate(candidates)

        candidatesWeekBeforeAnonymizationWithOldDate.forEach { cand ->
                sendMail(cand, printBodyDateWillExpire(cand))
        }

        newCandidatesWeekBeforeAnonymizationWithoutDate.forEach { cand ->
                sendMail(cand, printBodyWithoutDate(cand))
        }
    }

    private fun filterCandidatesWeekBeforeAnonymizationWithOldDate(candidates: List<CandidateDO>): List<CandidateDO> {
        return candidates.filter {
            it.consentValidTo != null
                    && it.consentValidTo.isEqual(LocalDate.now().plusDays(DAYS_UNTIL_ANONYMIZATION_WITH_OLD_DATE))
        }
    }

    private fun filterOutNewCandidatesWithoutDate(candidates: List<CandidateDO>): List<CandidateDO> {
        return candidates.filter {
            it.dateCreated.toLocalDate().isEqual(LocalDate.now().minusWeeks(WEEKS_TOLERANCE))
                    && it.consentValidTo == null
                    && !it.isAnonymized
                    && it.status != CandidateDO.CandidateStatus.HIRED
        }
    }

    private fun sendMail(candidateDO: CandidateDO, body: String) = try {
        mailer.sendMail(getRecipients(candidateDO), SUBJECT,
                HtmlReportBuilder.newInstance()
                        .headline(SUBJECT)
                        .body(body)
                        .build())
    } catch (e: MailerException) {
        log.warn("An error occured while trying to send an email!", e)
    }

    private fun getRecipients(candidateDO: CandidateDO) = candidateDO.branch.hrRecipients

    private fun printBodyDateWillExpire(candidateDO: CandidateDO) = BODY_DATE_WILL_EXPIRE.format(
    candidateDO.fullName,
            candidateDO.consentValidTo.toString(DATE_PATTERN)
            )

    private fun printBodyWithoutDate(candidateDO: CandidateDO) = BODY_NO_DATE.format(
    candidateDO.fullName,
            candidateDO.dateCreated.plusWeeks(WEEKS_SINCE_CREATION_TO_ANONYMIZATION).toString(DATE_PATTERN)
  )

}

