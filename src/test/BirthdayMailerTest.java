
class BirthdayMailerTest {

    private val birthdayMailer = spyk<BirthdayMailer>()
    private val mailer = mockk<Mailer>()
    private val htmlReportBuilder = mockk<HtmlReportBuilder>()
    private val userOperations = mockk<UserOperations>()

    companion object {
        private const val USER_FIRST_NAME = "Test"
        private const val USER_LAST_NAME = "Testovací"
        private const val USER_FULL_NAME = "$USER_LAST_NAME $USER_FIRST_NAME"
        private const val USER_EMAIL = "test.testovaci@artin.cz"
        private const val SUBJECT = "Narozeniny $USER_FULL_NAME";
        private val ACTUAL_DATE = LocalDate.parse("2022-04-21")
        private val USER_BIRTHDAY = LocalDate.parse("1990-04-22")
        private val USER_NEXT_AGE = Years.yearsBetween(USER_BIRTHDAY, ACTUAL_DATE).years + 1
        private val BODY = "Zítra, tj. 22. 4. oslaví $USER_FULL_NAME své $USER_NEXT_AGE. narozeniny. Nezapomeň mu/jí popřát."
        private val USERS_LIST = mutableListOf(createUser())

        private fun createUser(): UserDO {
            val user = UserDO()
            user.firstName = USER_FIRST_NAME
            user.lastName = USER_LAST_NAME
            user.birthday = USER_BIRTHDAY
            user.personalLeader = createPL()
            user.homeBranch = createHB()
            return user
        }

        private fun createPL(): UserDO {
            val user = UserDO()
            user.email = USER_EMAIL
            return user
        }

        private fun createHB(): BranchDO {
            val branch = BranchDO()
            branch.usingBirthdayNotification = true
            branch.countryCode = CountryCode.CZ
            return branch
        }
    }

    @Before
    fun setUp() {
        val today = ACTUAL_DATE
        mockkStatic(LocalDate::class)
        every { LocalDate.now() } returns today
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `mailer sends a correct mail`() {
        val body = slot<String>()
        val emails = slot<List<String>>()

        mockkStatic(HtmlReportBuilder::class)
        every { HtmlReportBuilder.newInstance() } returns htmlReportBuilder
        every { htmlReportBuilder.body(capture(body)) } returns htmlReportBuilder
        every { htmlReportBuilder.build() } returns ""
        every { mailer.sendMail(capture(emails), any(), any()) } just Runs
        mockkStatic(Aca::class)
        every { Aca.authAsFirstAdmin() } just Runs

        Whitebox.setInternalState(birthdayMailer, "mailer", mailer)
        Whitebox.setInternalState(birthdayMailer, "userOperations", userOperations)
        birthdayMailer.sendNotificationDependingOnCountry(USERS_LIST)

        Assert.assertEquals(BODY, body.captured)
        Assert.assertEquals(USER_EMAIL, emails.captured[0])

        verify { HtmlReportBuilder.newInstance() }
        verify { htmlReportBuilder.body(BODY) }
        verify { htmlReportBuilder.build() }
        verify { mailer.sendMail(listOf(USER_EMAIL), SUBJECT, "") }
    }

    @Test
    fun `mailer doesn't send an email when there are no users, who have birthday tomorrow`() {
    mockkStatic(HtmlReportBuilder::class)
    every { HtmlReportBuilder.newInstance() } returns htmlReportBuilder
    mockkStatic(Aca::class)
    every { Aca.authAsFirstAdmin() } just Runs

    Whitebox.setInternalState(birthdayMailer, "mailer", mailer)
            Whitebox.setInternalState(birthdayMailer, "userOperations", userOperations)
            birthdayMailer.sendNotificationDependingOnCountry(listOf<UserDO>())

    verify { htmlReportBuilder wasNot Called }
    verify { mailer wasNot Called }
}

}

