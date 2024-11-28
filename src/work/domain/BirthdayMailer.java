package work.domain;

    @Service
    class BirthdayMailer {

        companion object {
            private const val URL_USER_PREFIX = "https://tm.test.cz/ng/hr/employees/"

            private const val CZ_SUBJECT = "Narozeniny %s"
            private const val CZ_BODY = " %d. %d. oslaví %s své %d. narozeniny. Nezapomeň mu/jí popřát."
            private const val SK_SUBJECT = "Narodeniny %s"
            private const val SK_BODY = " %d. %d. oslávi %s svoje %d. narodeniny. Nezabudni mu/jej zablahoželať."
            private const val CZ_TOMMOROW = "Zítra, tj."
            private const val CZ_MONDAY = "V pondělí, tj."
            private const val CZ_TUESDAY = "V úterý, tj."
            private const val CZ_WEDNESDAY = "Ve středu, tj."
            private const val CZ_THURSDAY = "Ve čtvrtek, tj."
            private const val CZ_FRIDAY = "V pátek, tj."
            private const val CZ_WEEKEND = "O víkendu, tj."
            private const val CZ_DAY = "Dne"
            private const val SK_TOMMOROW = "Zajtra, tj."
            private const val SK_MONDAY = "V pondelok, tj."
            private const val SK_TUESDAY = "V utorok, tj."
            private const val SK_WEDNESDAY = "V stredu, tj."
            private const val SK_THURSDAY = "Vo štvrtok, tj."
            private const val SK_FRIDAY = "V piatok, tj."
            private const val SK_WEEKEND = "Cez víkend, tj."
            private const val SK_DAY = "Dňa"

            @Suppress("JAVA_CLASS_ON_COMPANION")
            @JvmStatic
            private val log = LoggerFactory.getLogger(javaClass.enclosingClass)
        }

        @Autowired
        private lateinit var mailer: Mailer

        @Autowired
        private lateinit var userOperations: UserOperations

        @Autowired
        private lateinit var employeeService: EmployeeService

        @Autowired
        private lateinit var branchService: BranchService


        @Transactional
        fun sendNotification() {
            Aca.authAsFirstAdmin()
            sendNotificationDependingOnCountry(czUsersWithBirthdayToSend)
            sendNotificationDependingOnCountry(skUsersWithBirthdayToSend)
        }

        fun sendNotificationDependingOnCountry(usersWithBirthdayToSend: List<UserDO>) {
            usersWithBirthdayToSend.forEach { user ->
                if (user.homeBranch.usingBirthdayNotification) {
                    val emailAdresses = getEmailAdressesToSend(user)
                    val emailSubject = String.format(
                    if (user.homeBranch.countryCode.equals(CountryCode.CZ)) CZ_SUBJECT else SK_SUBJECT,
                            user.fullName
        )

                    val czFilterMailDays = when {
                        isWorkDayTodayAndTommorow(CountryCode.CZ) -> when (user.birthday.plusYears(user.ageInYears + 1).dayOfWeek) {
                            DateUtils.currentDate().plusDays(1).dayOfWeek -> CZ_TOMMOROW
                            DateTimeConstants.SATURDAY -> CZ_WEEKEND
                            DateTimeConstants.SUNDAY -> CZ_WEEKEND
            else -> CZ_DAY
                        }

                        isNonWorkDayTommorow(CountryCode.CZ) -> when (user.birthday.plusYears(user.ageInYears + 1).dayOfWeek) {
                            DateTimeConstants.MONDAY -> CZ_MONDAY
                            DateTimeConstants.TUESDAY -> CZ_TUESDAY
                            DateTimeConstants.WEDNESDAY -> CZ_WEDNESDAY
                            DateTimeConstants.THURSDAY -> CZ_THURSDAY
                            DateTimeConstants.FRIDAY -> CZ_FRIDAY
            else -> CZ_WEEKEND
                        }
          else -> CZ_DAY
                    }

                    val skFilterMailDays = when {
                        isWorkDayTodayAndTommorow(CountryCode.SK) -> when (user.birthday.plusYears(user.ageInYears + 1).dayOfWeek) {
                            DateUtils.currentDate().plusDays(1).dayOfWeek -> SK_TOMMOROW
                            DateTimeConstants.SATURDAY -> SK_WEEKEND
                            DateTimeConstants.SUNDAY -> SK_WEEKEND
            else -> SK_DAY
                        }

                        isNonWorkDayTommorow(CountryCode.SK) -> when (user.birthday.plusYears(user.ageInYears + 1).dayOfWeek) {
                            DateTimeConstants.MONDAY -> SK_MONDAY
                            DateTimeConstants.TUESDAY -> SK_TUESDAY
                            DateTimeConstants.WEDNESDAY -> SK_WEDNESDAY
                            DateTimeConstants.THURSDAY -> SK_THURSDAY
                            DateTimeConstants.FRIDAY -> SK_FRIDAY
            else -> SK_WEEKEND
                        }
          else -> SK_DAY
                    }

                    val emailBody = String.format(
                    if (user.homeBranch.countryCode.equals(CountryCode.CZ)) czFilterMailDays + CZ_BODY
                    else skFilterMailDays + SK_BODY,
                            user.birthday.dayOfMonth,
                            user.birthday.monthOfYear,
                            user.fullName,
                            getAgeInYears(user.birthday)
        )

                    if (emailAdresses.isNotEmpty()) {
                        try {
                            mailer.sendMail(emailAdresses, emailSubject, HtmlReportBuilder.newInstance().body(emailBody).build())
                        } catch (e: MailerException) {
                            log.warn("An error occured while trying to send an email!", e)
                        }
                    }
                }
            }
        }

        private fun getAgeInYears(birthday: LocalDate) = LocalDate.now().getYear() - birthday.getYear();

        private val czUsersWithBirthdayToSend: List<UserDO>
        get() {
            return getUsersWithBirthdayToSendByCountryCode(CountryCode.CZ)
        }

        private val skUsersWithBirthdayToSend: List<UserDO>
        get() {
            return getUsersWithBirthdayToSendByCountryCode(CountryCode.SK)
        }

        private fun isWorkDayTodayAndTommorow(countryCode: CountryCode): Boolean {
            return (DateUtils.isWorkDay(DateUtils.currentDate(), countryCode)
                    && (DateUtils.isWorkDay(DateUtils.currentDate().plusDays(1), countryCode)))
        }

        private fun isNonWorkDayTommorow(countryCode: CountryCode): Boolean {
            return (!DateUtils.isWorkDay(DateUtils.currentDate().plusDays(1), countryCode))
        }

        private fun getUsersWithBirthdayToSendByCountryCode(countryCode: CountryCode): ArrayList<UserDO> {
            val usersWithBirthdayToSend = arrayListOf<UserDO>()
            val userFilter = UserFilterDTO()
                    .setActive(true)
                    .setCustomer(false)
                    .setSystemAccount(false)
                    .setCountryCode(countryCode)
            if (!DateUtils.isWorkDay(DateUtils.currentDate(), countryCode)) {
                return usersWithBirthdayToSend
            }

            //Get next working day after working day
            if (isWorkDayTodayAndTommorow(countryCode)) {
                userFilter.birthday = DateUtils.currentDate().plusDays(1)
                usersWithBirthdayToSend.addAll(userOperations.getUsers(userFilter))
            }

            var daysAfterCurrent = 2;
            //Get weekends and holidays
            if (!DateUtils.isWorkDay(DateUtils.currentDate().plusDays(daysAfterCurrent), countryCode)) {
                while (!DateUtils.isWorkDay(DateUtils.currentDate().plusDays(daysAfterCurrent), countryCode)) {
                    //Not to send an email on Friday
                    if (!DateUtils.isWorkDay(DateUtils.currentDate().plusDays(1), countryCode)) {
                        daysAfterCurrent++
                    } else {
                        userFilter.birthday = DateUtils.currentDate().plusDays(daysAfterCurrent)
                        usersWithBirthdayToSend.addAll(userOperations.getUsers(userFilter))
                        daysAfterCurrent++
                    }
                }
            }

            //Working day between non-working days
            //Get birthday next non-working day(s) and first working day after non-working day
            var nextDay = 1
            if (!DateUtils.isWorkDay(DateUtils.currentDate().minusDays(1), countryCode)
                    && (!DateUtils.isWorkDay(DateUtils.currentDate().plusDays(1), countryCode))
            ) {
                while (!DateUtils.isWorkDay(DateUtils.currentDate().plusDays(nextDay), countryCode)) {
                    userFilter.birthday = DateUtils.currentDate().plusDays(nextDay)
                    usersWithBirthdayToSend.addAll(userOperations.getUsers(userFilter))
                    nextDay++
                }
            }

            //Get first working day after non-working day
            if (isNonWorkDayTommorow(countryCode)) {
                userFilter.birthday = DateUtils.currentDate().plusDays(daysAfterCurrent)
                usersWithBirthdayToSend.addAll(userOperations.getUsers(userFilter))
            }
            return usersWithBirthdayToSend
        }

        private fun getEmailAdressesToSend(user: UserDO): List<String> {
            val emailAdresses = arrayListOf<String>()
            emailAdresses.addAll(user.homeBranch.birthdayNotificationRecipients)
            if (user.personalLeader != null) {
                emailAdresses.add(user.personalLeader.email)
            }
            return emailAdresses
        }
    }

}
