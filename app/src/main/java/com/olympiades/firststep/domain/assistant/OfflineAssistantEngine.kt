package com.olympiades.firststep.domain.assistant

import com.olympiades.firststep.domain.model.AppLanguage
import com.olympiades.firststep.domain.model.ApplicationStatus
import com.olympiades.firststep.domain.model.ApplicationWithDetails
import com.olympiades.firststep.domain.model.DateTimeHelper
import com.olympiades.firststep.domain.model.FavoriteOffer
import com.olympiades.firststep.domain.model.Offer
import com.olympiades.firststep.domain.model.OfferStatus
import com.olympiades.firststep.domain.model.SearchTextNormalizer
import com.olympiades.firststep.domain.model.StudentProfile

enum class AssistantTopic {
    HOW_TO_SEARCH_AND_FILTER,
    HOW_TO_APPLY,
    HOW_TO_FAVORITES,
    HOW_TO_TRACK_APPLICATIONS,
    HOW_TO_PROFILE_AND_PIN,
    RECOMMEND_BEST_OFFERS,
    CHECK_MY_APPLICATIONS,
    TROUBLESHOOTING
}

enum class AssistantNavTarget {
    OFFERS,
    FAVORITES,
    APPLICATIONS,
    PROFILE
}

data class AssistantQuickQuestion(
    val topic: AssistantTopic,
    val label: String
)

data class AssistantOfferSuggestion(
    val offerId: Long,
    val title: String,
    val subtitle: String
)

data class AssistantActionShortcut(
    val label: String,
    val target: AssistantNavTarget
)

data class AssistantResponse(
    val message: String,
    val suggestedOffers: List<AssistantOfferSuggestion> = emptyList(),
    val actionShortcut: AssistantActionShortcut? = null,
    val followUpQuestions: List<AssistantQuickQuestion> = emptyList()
)

data class AssistantContext(
    val language: AppLanguage,
    val currentRoute: String?,
    val offers: List<Offer>,
    val favorites: List<FavoriteOffer>,
    val applications: List<ApplicationWithDetails>,
    val profile: StudentProfile?,
    val todayIso: String = DateTimeHelper.todayIso()
)

object OfflineAssistantEngine {

    private val stopWords = setOf(
        "comment", "puis", "je", "tu", "vous", "on", "quoi", "quel", "quelle", "quels", "quelles",
        "est", "ce", "que", "de", "du", "des", "la", "le", "les", "un", "une", "en", "pour",
        "dans", "sur", "avec", "sans", "et", "ou", "mon", "ma", "mes", "a", "au", "aux", "faire",
        "peux", "veux", "aide", "aider", "besoin", "savoir", "utiliser", "application", "app",
        "how", "can", "i", "the", "to", "in", "my", "is", "are", "for", "me", "help", "with",
        "what", "where", "why", "do", "does", "show", "find",
        "كيف", "يمكن", "هل", "ما", "هو", "هي", "في", "من", "الي", "على", "عن", "مع", "لي", "انا"
    )

    private data class TopicKeywordRule(
        val topic: AssistantTopic,
        val keywords: List<String>
    )

    private val keywordRules: List<TopicKeywordRule> = listOf(
        TopicKeywordRule(
            topic = AssistantTopic.HOW_TO_APPLY,
            keywords = listOf(
                "postul", "candidat", "formulaire", "cv", "pdf", "lettre", "motiv", "envoyer",
                "apply", "submit", "resume", "cover", "application form",
                "تقديم", "ترشح", "استماره", "سيره", "ذاتيه", "ارفاق", "ارسال", "دافع"
            )
        ),
        TopicKeywordRule(
            topic = AssistantTopic.HOW_TO_SEARCH_AND_FILTER,
            keywords = listOf(
                "cherch", "recherch", "filtr", "wilaya", "ville", "domaine", "tri", "trie", "25000", "25 000",
                "search", "filter", "city", "domain", "sort", "states",
                "بحث", "فلتر", "تصفية", "ولايه", "مدينه", "مجال", "ترتيب"
            )
        ),
        TopicKeywordRule(
            topic = AssistantTopic.HOW_TO_FAVORITES,
            keywords = listOf(
                "favori", "coeur", "memo", "annul", "note", "enregistr", "sauvegard",
                "heart", "bookmark", "favorite", "undo", "save note",
                "مفضله", "قلب", "ملاحظه", "حفظ", "تراجع"
            )
        ),
        TopicKeywordRule(
            topic = AssistantTopic.HOW_TO_TRACK_APPLICATIONS,
            keywords = listOf(
                "suiv", "statut", "historique", "relance", "7j", "7 j", "jours", "csv", "export",
                "accept", "refus", "verrou", "final", "track", "status", "history", "timeline",
                "تتبع", "حاله", "تصدير", "متابعه", "تحديث"
            )
        ),
        TopicKeywordRule(
            topic = AssistantTopic.HOW_TO_PROFILE_AND_PIN,
            keywords = listOf(
                "profil", "param", "langue", "arabe", "francais", "anglais", "theme", "sombre", "clair",
                "pin", "code", "securit", "lock", "profile", "dark", "light", "language", "english", "setting",
                "حساب", "ملف", "لغه", "مظهر", "داكن", "امان", "قفل", "رمز"
            )
        ),
        TopicKeywordRule(
            topic = AssistantTopic.RECOMMEND_BEST_OFFERS,
            keywords = listOf(
                "meilleur", "recommand", "conseil", "stage", "offre", "paye", "remuner", "gratification",
                "salaire", "da", "dzd", "ouvert", "best", "recommend", "paid", "offer", "stipend", "internship",
                "افضل", "ترشيح", "اقترح", "عرض", "تربص", "مدفوع", "منحه", "مفتوح"
            )
        ),
        TopicKeywordRule(
            topic = AssistantTopic.CHECK_MY_APPLICATIONS,
            keywords = listOf(
                "mes candid", "bilan", "resume", "combien", "statistique", "en attente", "retard",
                "my application", "summary", "check", "overdue", "alert",
                "ملخص", "ترشحاتي", "احصائيات", "كم", "تذكير"
            )
        ),
        TopicKeywordRule(
            topic = AssistantTopic.TROUBLESHOOTING,
            keywords = listOf(
                "pourquoi", "marche pas", "bloque", "desactiv", "gris", "modifier", "editer", "erreur",
                "hors ligne", "offline", "internet", "pc", "stylo", "bug", "problem", "locked", "cant", "why", "disabled",
                "لماذا", "مشكله", "معطل", "مقفل", "تعديل", "انترنت", "بدون", "خطا"
            )
        )
    )

    private inline fun <T> tr(lang: AppLanguage, fr: () -> T, ar: () -> T, en: () -> T): T =
        when (lang) {
            AppLanguage.FR -> fr()
            AppLanguage.AR -> ar()
            AppLanguage.EN -> en()
        }

    fun welcomeMessage(context: AssistantContext): AssistantResponse {
        val openCount = context.offers.count { it.status == OfferStatus.OPEN && !it.isExpired(context.todayIso) }
        val overdueCount = context.applications.count { it.isUnansweredOver7Days(context.todayIso) }
        val studentName = context.profile?.fullName?.trim()?.takeIf { it.isNotBlank() }

        val message = tr(
            context.language,
            fr = {
                buildString {
                    if (studentName != null) append("Bonjour $studentName ! ") else append("Bonjour ! ")
                    append("Je suis l'assistant hors-ligne FIRSTSTEP (100 % local, sans connexion Internet).\n\n")
                    append("• Offres ouvertes actuellement : $openCount\n")
                    append("• Favoris : ${context.favorites.size} | Candidatures : ${context.applications.size}")
                    if (overdueCount > 0) {
                        append("\n• Alerte : $overdueCount candidature(s) sans réponse depuis plus de 7 jours.")
                    }
                    append("\n\nAppuyez sur un bouton question ci-dessous ou posez votre question :")
                }
            },
            ar = {
                buildString {
                    if (studentName != null) append("مرحباً $studentName! ") else append("مرحباً بك! ")
                    append("أنا مساعد FIRSTSTEP الذكي (يعمل 100% بدون إنترنت على بيانات جهازك).\n\n")
                    append("• العروض المفتوحة حالياً: $openCount\n")
                    append("• المفضلة: ${context.favorites.size} | الترشحات: ${context.applications.size}")
                    if (overdueCount > 0) {
                        append("\n• تنبيه: لديك $overdueCount ترشح(ات) بدون رد منذ أكثر من 7 أيام.")
                    }
                    append("\n\nاضغط على أحد أزرار الأسئلة أدناه أو اكتب سؤالك هنا:")
                }
            },
            en = {
                buildString {
                    if (studentName != null) append("Hello $studentName! ") else append("Hello! ")
                    append("I'm the FIRSTSTEP offline assistant (100% local, zero internet needed).\n\n")
                    append("• Currently open offers: $openCount\n")
                    append("• Favorites: ${context.favorites.size} | Applications: ${context.applications.size}")
                    if (overdueCount > 0) {
                        append("\n• Alert: $overdueCount application(s) unanswered for over 7 days.")
                    }
                    append("\n\nTap a quick question button below or type your question:")
                }
            }
        )

        return AssistantResponse(
            message = message,
            followUpQuestions = quickQuestionsFor(context)
        )
    }

    fun quickQuestionsFor(context: AssistantContext): List<AssistantQuickQuestion> {
        val lang = context.language
        val labels = mapOf(
            AssistantTopic.HOW_TO_SEARCH_AND_FILTER to tr(
                lang,
                fr = { "Comment filtrer les 58 wilayas ?" },
                ar = { "كيف أبحث وأفلتر العروض؟" },
                en = { "How to filter 58 wilayas?" }
            ),
            AssistantTopic.HOW_TO_APPLY to tr(
                lang,
                fr = { "Comment postuler avec CV PDF ?" },
                ar = { "كيف أقدّم على تربص؟" },
                en = { "How to apply with a PDF CV?" }
            ),
            AssistantTopic.RECOMMEND_BEST_OFFERS to tr(
                lang,
                fr = { "Meilleures offres rémunérées ?" },
                ar = { "أفضل العروض المدفوعة حالياً" },
                en = { "Best paid offers right now?" }
            ),
            AssistantTopic.CHECK_MY_APPLICATIONS to tr(
                lang,
                fr = { "Bilan de mes candidatures ?" },
                ar = { "ملخص ترشحاتي وتنبيه >7 أيام" },
                en = { "My applications & >7d alerts" }
            ),
            AssistantTopic.HOW_TO_FAVORITES to tr(
                lang,
                fr = { "Utiliser favoris & mémos ?" },
                ar = { "كيف أحفظ المفضلة والملاحظات؟" },
                en = { "Use favorites & notes?" }
            ),
            AssistantTopic.HOW_TO_TRACK_APPLICATIONS to tr(
                lang,
                fr = { "Suivi statuts & export CSV ?" },
                ar = { "تتبع الحالات وتصدير CSV" },
                en = { "Track statuses & CSV export" }
            ),
            AssistantTopic.HOW_TO_PROFILE_AND_PIN to tr(
                lang,
                fr = { "Langue FR/EN/AR, thème & PIN ?" },
                ar = { "اللغة، المظهر وقفل PIN" },
                en = { "Language, theme & PIN lock?" }
            ),
            AssistantTopic.TROUBLESHOOTING to tr(
                lang,
                fr = { "Bouton grisé ou verrouillé ?" },
                ar = { "لماذا بعض الأزرار مقفلة؟" },
                en = { "Why is a button locked?" }
            )
        )

        val prioritizedOrder = when {
            context.currentRoute?.contains("application", ignoreCase = true) == true -> listOf(
                AssistantTopic.CHECK_MY_APPLICATIONS,
                AssistantTopic.HOW_TO_TRACK_APPLICATIONS,
                AssistantTopic.HOW_TO_APPLY,
                AssistantTopic.TROUBLESHOOTING,
                AssistantTopic.RECOMMEND_BEST_OFFERS,
                AssistantTopic.HOW_TO_PROFILE_AND_PIN
            )
            context.currentRoute?.contains("favorites", ignoreCase = true) == true -> listOf(
                AssistantTopic.HOW_TO_FAVORITES,
                AssistantTopic.HOW_TO_APPLY,
                AssistantTopic.RECOMMEND_BEST_OFFERS,
                AssistantTopic.CHECK_MY_APPLICATIONS,
                AssistantTopic.HOW_TO_SEARCH_AND_FILTER
            )
            context.currentRoute?.contains("profile", ignoreCase = true) == true -> listOf(
                AssistantTopic.HOW_TO_PROFILE_AND_PIN,
                AssistantTopic.CHECK_MY_APPLICATIONS,
                AssistantTopic.HOW_TO_TRACK_APPLICATIONS,
                AssistantTopic.TROUBLESHOOTING,
                AssistantTopic.RECOMMEND_BEST_OFFERS
            )
            context.currentRoute?.contains("offer_detail", ignoreCase = true) == true -> listOf(
                AssistantTopic.HOW_TO_APPLY,
                AssistantTopic.TROUBLESHOOTING,
                AssistantTopic.HOW_TO_FAVORITES,
                AssistantTopic.RECOMMEND_BEST_OFFERS,
                AssistantTopic.CHECK_MY_APPLICATIONS
            )
            else -> listOf(
                AssistantTopic.HOW_TO_SEARCH_AND_FILTER,
                AssistantTopic.HOW_TO_APPLY,
                AssistantTopic.RECOMMEND_BEST_OFFERS,
                AssistantTopic.CHECK_MY_APPLICATIONS,
                AssistantTopic.HOW_TO_TRACK_APPLICATIONS,
                AssistantTopic.HOW_TO_PROFILE_AND_PIN,
                AssistantTopic.HOW_TO_FAVORITES,
                AssistantTopic.TROUBLESHOOTING
            )
        }

        return prioritizedOrder.mapNotNull { topic ->
            labels[topic]?.let { AssistantQuickQuestion(topic, it) }
        }
    }

    fun answerTopic(topic: AssistantTopic, context: AssistantContext): AssistantResponse {
        val lang = context.language
        return when (topic) {
            AssistantTopic.HOW_TO_SEARCH_AND_FILTER -> AssistantResponse(
                message = tr(
                    lang,
                    fr = {
                        "Pour trouver un stage dans l'onglet « Accueil » :\n" +
                            "1. Saisissez un mot-clé (titre, entreprise, wilaya ou compétence) en FR ou AR.\n" +
                            "2. Combinez les filtres rapides : « Payé », « ≥ 25 000 DA » et « Ouvert ».\n" +
                            "3. Choisissez un « Domaine » ou une « Ville » parmi les 58 wilayas d'Algérie.\n" +
                            "4. Triez par « Plus récentes », « Date limite proche » ou « Gratification max »."
                    },
                    ar = {
                        "للبحث عن تربص في تبويب «العروض»:\n" +
                            "1. اكتب أي كلمة مفتاحية (عنوان، شركة، مدينة أو مهارة) بالعربية أو الفرنسية.\n" +
                            "2. فعّل شرائح الفلترة القابلة للجمع: «مدفوع»، «≥ 25,000 دج»، و«مفتوح».\n" +
                            "3. اختر «المجال» أو «المدينة» للبحث في جميع الولايات الجزائرية الـ 58.\n" +
                            "4. بدّل الترتيب عبر زر الفرز (الأحدث، الأقرب أجلاً، أو الأعلى منحة)."
                    },
                    en = {
                        "How to find an internship on the Home tab:\n" +
                            "1. Enter any keyword (title, company, wilaya, or skill).\n" +
                            "2. Combine quick filter chips: \"Paid\", \"≥ 25,000 DZD\", and \"Open\".\n" +
                            "3. Filter by Field or any of the 58 Algerian Wilayas.\n" +
                            "4. Sort by Newest, Closest deadline, or Highest stipend."
                    }
                ),
                actionShortcut = shortcut(AssistantNavTarget.OFFERS, lang),
                followUpQuestions = followUps(context, exclude = topic)
            )

            AssistantTopic.HOW_TO_APPLY -> AssistantResponse(
                message = tr(
                    lang,
                    fr = {
                        "Comment envoyer une candidature hors-ligne :\n" +
                            "1. Ouvrez une offre ouverte puis appuyez sur « POSTULER » en bas de l'écran.\n" +
                            "2. Vos coordonnées du « Profil » sont pré-remplies automatiquement.\n" +
                            "3. Rédigez votre lettre de motivation (min. 10 caractères) et joignez un CV PDF.\n" +
                            "4. Validez pour obtenir une référence unique (FS-2026-XXXX) enregistrée dans SQLite."
                    },
                    ar = {
                        "خطوات إرسال طلب ترشح:\n" +
                            "1. افتح أي عرض مفتوح واضغط على زر «تقديم طلب» في الأسفل.\n" +
                            "2. يتم ملء بياناتك تلقائياً من «حسابي» (الاسم، البريد، الهاتف).\n" +
                            "3. اكتب رسالة الدافع (10 أحرف على الأقل) وأرفق سيرتك الذاتية بصيغة PDF عند الحاجة.\n" +
                            "4. اضغط «إرسال الترشح» ليتم حفظه في SQLite برقم مرجعي فوري مثل FS-2026-0001."
                    },
                    en = {
                        "How to submit an application offline:\n" +
                            "1. Open an open offer and tap \"APPLY NOW\" at the bottom.\n" +
                            "2. Your details from Profile are pre-filled automatically.\n" +
                            "3. Write your cover letter (min. 10 characters) and optionally attach a PDF CV.\n" +
                            "4. Submit to get an instant reference (FS-2026-XXXX) saved locally in SQLite."
                    }
                ),
                suggestedOffers = topOpenOffers(context, limit = 2),
                followUpQuestions = followUps(context, exclude = topic)
            )

            AssistantTopic.HOW_TO_FAVORITES -> AssistantResponse(
                message = tr(
                    lang,
                    fr = {
                        "Gérer vos favoris et mémos :\n" +
                            "• Touchez le cœur ❤️ sur une offre ou dans son détail pour l'enregistrer localement.\n" +
                            "• Dans l'onglet « Favoris », ajoutez une note personnelle et touchez l'icône Disquette 💾.\n" +
                            "• Si vous retirez un favori par erreur, touchez « Annuler » dans la barre de confirmation."
                    },
                    ar = {
                        "إدارة المفضلة والملاحظات الشخصية:\n" +
                            "• اضغط على أيقونة القلب ❤️ في بطاقة العرض أو صفحة التفاصيل لحفظه دون إنترنت.\n" +
                            "• في تبويب «المفضلة»، اكتب ملاحظة شخصية لكل عرض واضغط أيقونة الحفظ 💾 لتسجيلها في SQLite.\n" +
                            "• عند الإزالة عن طريق الخطأ، استعمل زر «تراجع» الذي يظهر فوراً في الأسفل."
                    },
                    en = {
                        "Managing favorites & personal notes:\n" +
                            "• Tap the heart ❤️ on any offer card or detail page to save it offline.\n" +
                            "• On the Favorites tab, type a personal memo and tap Save 💾 to store it in SQLite.\n" +
                            "• Removed one by mistake? Tap \"Undo\" in the confirmation bar."
                    }
                ),
                actionShortcut = shortcut(AssistantNavTarget.FAVORITES, lang),
                followUpQuestions = followUps(context, exclude = topic)
            )

            AssistantTopic.HOW_TO_TRACK_APPLICATIONS -> AssistantResponse(
                message = tr(
                    lang,
                    fr = {
                        "Suivi des candidatures et export CSV :\n" +
                            "• Touchez une carte dans « Candid. » pour voir vos infos, les modifier (avant statut final) et consulter l'historique.\n" +
                            "• Mettez à jour le statut (En examen, Entretien, Acceptée, Refusée, Annulée). Les statuts finaux sont verrouillés.\n" +
                            "• Les demandes « Soumise » sans réponse > 7 jours affichent une alerte orange.\n" +
                            "• Touchez l'icône ⬇ en haut à droite pour exporter les candidatures filtrées en CSV sécurisé."
                    },
                    ar = {
                        "متابعة الترشحات والتصدير في تبويب «ترشحاتي»:\n" +
                            "• اضغط على أي بطاقة لعرض تفاصيل الترشح، تعديل بياناتك قبل الإغلاق، ورؤية السجل الزمني.\n" +
                            "• حدّث الحالة (قيد الدراسة، مقابلة، مقبولة، مرفوضة، ملغاة). الحالات النهائية تُقفل نهائياً لحماية السجل.\n" +
                            "• الترشحات المرسلة منذ أكثر من 7 أيام بدون رد تظهر مع شارة تنبيه برتقالية.\n" +
                            "• اضغط أيقونة التحميل ⬇ في الشريط العلوي لتصدير القائمة المفلترة إلى ملف CSV آمن."
                    },
                    en = {
                        "Tracking applications & CSV export:\n" +
                            "• Tap any card on the Apps tab to view saved details, edit info (before final status), and inspect the timeline.\n" +
                            "• Update status (Under Review, Interview, Accepted, Rejected, Cancelled). Final states lock the record.\n" +
                            "• Submitted applications unanswered > 7 days show an orange follow-up badge.\n" +
                            "• Tap the download icon ⬇ in the top bar to export filtered applications to a safe CSV file."
                    }
                ),
                actionShortcut = shortcut(AssistantNavTarget.APPLICATIONS, lang),
                followUpQuestions = followUps(context, exclude = topic)
            )

            AssistantTopic.HOW_TO_PROFILE_AND_PIN -> AssistantResponse(
                message = tr(
                    lang,
                    fr = {
                        "Profil, langue et sécurité locale :\n" +
                            "• Dans « Profil », touchez « Modifier le profil » pour mettre à jour vos coordonnées (pré-remplies lors des candidatures).\n" +
                            "• Basculez la langue FR / EN / AR (interface RTL pour l'arabe) et le thème Clair / Sombre.\n" +
                            "• Activez le « Verrouillage PIN (4 chiffres) » pour protéger vos données locales par hachage SHA-256 salé."
                    },
                    ar = {
                        "الملف الشخصي، اللغة والأمان:\n" +
                            "• في تبويب «حسابي»، اضغط «تعديل الملف الشخصي» لتحديث الجامعة والتخصص والبيانات التي تُستخدم تلقائياً عند التقديم.\n" +
                            "• بدّل اللغة بين FR و EN و AR (مع قلب الاتجاه RTL تلقائياً للعربية) والمظهر بين فاتح وداكن.\n" +
                            "• فعّل «القفل برمز PIN» لتأمين التطبيق برمز من 4 أرقام مشفر محلياً بتقنية SHA-256."
                    },
                    en = {
                        "Profile, language & local security:\n" +
                            "• On the Profile tab, tap \"Edit profile\" to update your details (auto-filled in application forms).\n" +
                            "• Switch between FR / EN / AR (full RTL layout for Arabic) and Light / Dark themes.\n" +
                            "• Enable the 4-digit PIN Lock to protect local data using salted SHA-256 hashing."
                    }
                ),
                actionShortcut = shortcut(AssistantNavTarget.PROFILE, lang),
                followUpQuestions = followUps(context, exclude = topic)
            )

            AssistantTopic.RECOMMEND_BEST_OFFERS -> buildRecommendedOffersResponse(context)

            AssistantTopic.CHECK_MY_APPLICATIONS -> buildApplicationsSummaryResponse(context)

            AssistantTopic.TROUBLESHOOTING -> AssistantResponse(
                message = tr(
                    lang,
                    fr = {
                        "Dépannage rapide :\n" +
                            "• Bouton « POSTULER » grisé ? L'offre est clôturée, expirée ou vous avez déjà postulé.\n" +
                            "• Pas de bouton d'édition sur les offres ? La modification du catalogue est réservée à l'administrateur depuis l'éditeur PC afin de protéger l'intégrité des offres.\n" +
                            "• Impossible de modifier une candidature ? Un statut final (Acceptée / Refusée / Annulée) verrouille définitivement la fiche.\n" +
                            "• Aucun réseau requis : 100 % des fonctionnalités s'exécutent hors-ligne sur votre appareil."
                    },
                    ar = {
                        "حلول سريعة لأكثر الأسئلة شيوعاً:\n" +
                            "• زر «تقديم طلب» معطل؟ يعني أن العرض مغلق، انتهى آخر أجل، أو أنك قدّمت عليه مسبقاً.\n" +
                            "• لا يوجد زر تعديل للعروض على الهاتف؟ عروض التربص مُدارة من طرف الإدارة عبر أداة قاعدة البيانات على الحاسوب لمنع التعديل العشوائي.\n" +
                            "• لا يظهر زر «تعديل البيانات» في الترشح؟ لأن الترشح وصل إلى حالة نهائية مقفلة (مقبولة/مرفوضة/ملغاة).\n" +
                            "• هل يحتاج التطبيق إلى إنترنت؟ لا، يعمل 100% محلياً بدون أي اتصال."
                    },
                    en = {
                        "Quick troubleshooting:\n" +
                            "• \"APPLY NOW\" disabled? The offer is closed, past its deadline, or you already applied.\n" +
                            "• Why can't I edit offer info on the phone? Course/offer details are managed from the PC database editor so mobile users can't alter official listings.\n" +
                            "• Can't edit an application? Final statuses (Accepted / Rejected / Cancelled) lock the record.\n" +
                            "• Offline guarantee: 100% of features run locally with zero internet access."
                    }
                ),
                followUpQuestions = followUps(context, exclude = topic)
            )
        }
    }

    fun answerQuestion(rawQuery: String, context: AssistantContext): AssistantResponse {
        val sanitized = SearchTextNormalizer.sanitizeInputQuery(rawQuery).trim()
        if (sanitized.isEmpty()) {
            return welcomeMessage(context)
        }

        val normalizedQuery = SearchTextNormalizer.normalizeSearchText(sanitized)
        val tokens = SearchTextNormalizer.tokenizeQuery(sanitized)
        val lang = context.language

        val bestTopicEntry = keywordRules
            .map { rule ->
                val score = rule.keywords.sumOf { kw ->
                    val normKw = SearchTextNormalizer.normalizeSearchText(kw)
                    when {
                        normKw.isNotBlank() && normalizedQuery.contains(normKw) -> 3
                        tokens.any { tok -> tok.length >= 3 && (normKw.contains(tok) || tok.contains(normKw)) } -> 1
                        else -> 0
                    }
                }
                rule.topic to score
            }
            .maxByOrNull { it.second }

        val meaningfulTokens = tokens.filter { it.length >= 2 && it !in stopWords }
        val matchedOffers = findMatchingOffers(meaningfulTokens, context)

        val matchedTopic = bestTopicEntry?.takeIf { it.second >= 3 }?.first
        if (matchedTopic != null) {
            val base = answerTopic(matchedTopic, context)
            if (matchedOffers.isNotEmpty()) {
                val extraHeader = tr(
                    lang,
                    fr = { "\n\nOffres correspondantes dans la base locale :" },
                    ar = { "\n\nعروض مطابقة في قاعدة البيانات:" },
                    en = { "\n\nMatching offers in local database:" }
                )
                return base.copy(
                    message = base.message + extraHeader,
                    suggestedOffers = matchedOffers
                )
            }
            return base
        }

        if (matchedOffers.isNotEmpty()) {
            return AssistantResponse(
                message = tr(
                    lang,
                    fr = { "Voici ${matchedOffers.size} offre(s) trouvée(s) localement pour votre demande. Touchez une offre pour voir les détails :" },
                    ar = { "وجدت ${matchedOffers.size} عروض مطابقة لسؤالك في قاعدة البيانات المحلية. اضغط على أي عرض لفتح تفاصيله:" },
                    en = { "Found ${matchedOffers.size} matching offer(s) locally. Tap any card to open its details:" }
                ),
                suggestedOffers = matchedOffers,
                actionShortcut = shortcut(AssistantNavTarget.OFFERS, lang),
                followUpQuestions = quickQuestionsFor(context).take(3)
            )
        }

        return AssistantResponse(
            message = tr(
                lang,
                fr = { "Je n'ai pas identifié exactement cette demande hors-ligne. Essayez un mot-clé (ex. « Oran », « Kotlin », « CV », « PIN », « CSV ») ou touchez directement un bouton question ci-dessous :" },
                ar = { "لم أجد إجابة دقيقة لهذا السؤال بدون اتصال. يمكنك تجربة البحث باسم ولاية أو مهارة (مثل «وهران» أو «Kotlin»)، أو الضغط على أحد أزرار المساعدة السريعة أدناه:" },
                en = { "I couldn't find an exact match offline. Try a keyword (like \"Oran\", \"Kotlin\", \"CV\", \"PIN\", \"CSV\") or tap one of the quick question buttons below:" }
            ),
            followUpQuestions = quickQuestionsFor(context).take(5)
        )
    }

    private fun buildRecommendedOffersResponse(context: AssistantContext): AssistantResponse {
        val lang = context.language
        val openOffers = context.offers.filter { it.status == OfferStatus.OPEN && !it.isExpired(context.todayIso) }
        val paidOpen = openOffers
            .filter { it.isPaid }
            .sortedByDescending { it.monthlyStipendDzd }

        val topSuggestions = (paidOpen.ifEmpty { openOffers })
            .take(3)
            .map { offer -> offer.toSuggestion(lang) }

        val msg = if (openOffers.isEmpty()) {
            tr(
                lang,
                fr = { "Aucune offre ouverte n'est disponible actuellement dans la base locale." },
                ar = { "لا توجد عروض مفتوحة حالياً في قاعدة البيانات المحلية." },
                en = { "There are currently no open offers in the local database." }
            )
        } else {
            tr(
                lang,
                fr = { "Il y a actuellement ${openOffers.size} offre(s) ouverte(s) dont ${paidOpen.size} rémunérée(s). Voici les meilleures opportunités du moment :" },
                ar = { "يوجد حالياً ${openOffers.size} عرض مفتوح (منها ${paidOpen.size} عرض مدفوع الأجر). إليك أبرز الترشيحات المقترحة لك:" },
                en = { "There are ${openOffers.size} open offer(s) right now (${paidOpen.size} paid). Here are the top recommendations for you:" }
            )
        }

        return AssistantResponse(
            message = msg,
            suggestedOffers = topSuggestions,
            actionShortcut = shortcut(AssistantNavTarget.OFFERS, lang),
            followUpQuestions = followUps(context, exclude = AssistantTopic.RECOMMEND_BEST_OFFERS)
        )
    }

    private fun buildApplicationsSummaryResponse(context: AssistantContext): AssistantResponse {
        val lang = context.language
        val apps = context.applications
        if (apps.isEmpty()) {
            return AssistantResponse(
                message = tr(
                    lang,
                    fr = {
                        "Vous n'avez encore envoyé aucune candidature (favoris enregistrés : ${context.favorites.size}).\n" +
                            "Ouvrez une offre ouverte et touchez « POSTULER » pour démarrer."
                    },
                    ar = {
                        "لم ترسل أي طلب ترشح حتى الآن. لديك ${context.favorites.size} عرض في المفضلة.\n" +
                            "افتح أي عرض مفتوح واضغط «تقديم طلب» لإرسال أول ترشح لك."
                    },
                    en = {
                        "You haven't submitted any applications yet (saved favorites: ${context.favorites.size}).\n" +
                            "Open an open offer and tap \"APPLY NOW\" to get started."
                    }
                ),
                suggestedOffers = topOpenOffers(context, limit = 2),
                actionShortcut = shortcut(AssistantNavTarget.OFFERS, lang),
                followUpQuestions = followUps(context, exclude = AssistantTopic.CHECK_MY_APPLICATIONS)
            )
        }

        val submitted = apps.count { it.currentStatus == ApplicationStatus.SUBMITTED }
        val inProgress = apps.count {
            it.currentStatus == ApplicationStatus.UNDER_REVIEW ||
                it.currentStatus == ApplicationStatus.INTERVIEW_SCHEDULED
        }
        val accepted = apps.count { it.currentStatus == ApplicationStatus.ACCEPTED }
        val overdueApps = apps.filter { it.isUnansweredOver7Days(context.todayIso) }

        val msg = tr(
            lang,
            fr = {
                buildString {
                    append("Bilan de vos candidatures (${apps.size} au total) :\n")
                    append("• Soumises : $submitted | En examen / Entretien : $inProgress | Acceptées : $accepted\n")
                    if (overdueApps.isNotEmpty()) {
                        append("\n⚠️ Alerte : ${overdueApps.size} candidature(s) sans réponse > 7 jours (${overdueApps.joinToString { it.reference }}). Pensez à relancer l'entreprise.")
                    } else {
                        append("\n✅ Aucune candidature en attente depuis plus de 7 jours.")
                    }
                }
            },
            ar = {
                buildString {
                    append("ملخص ترشحاتك (${apps.size} إجمالاً):\n")
                    append("• مُرسَلة: $submitted | قيد الدراسة/مقابلة: $inProgress | مقبولة: $accepted\n")
                    if (overdueApps.isNotEmpty()) {
                        append("\n⚠️ تنبيه: ${overdueApps.size} ترشح(ات) بدون رد منذ أكثر من 7 أيام (${overdueApps.joinToString { it.reference }}). يُنصح بالمتابعة أو تصدير السجل CSV.")
                    } else {
                        append("\n✅ لا توجد ترشحات متأخرة (+7 أيام) حالياً.")
                    }
                }
            },
            en = {
                buildString {
                    append("Your applications summary (${apps.size} total):\n")
                    append("• Submitted: $submitted | Under Review / Interview: $inProgress | Accepted: $accepted\n")
                    if (overdueApps.isNotEmpty()) {
                        append("\n⚠️ Alert: ${overdueApps.size} application(s) unanswered > 7 days (${overdueApps.joinToString { it.reference }}). Consider following up or exporting CSV.")
                    } else {
                        append("\n✅ No applications overdue (> 7 days).")
                    }
                }
            }
        )

        return AssistantResponse(
            message = msg,
            actionShortcut = shortcut(AssistantNavTarget.APPLICATIONS, lang),
            followUpQuestions = followUps(context, exclude = AssistantTopic.CHECK_MY_APPLICATIONS)
        )
    }

    private fun findMatchingOffers(tokens: List<String>, context: AssistantContext): List<AssistantOfferSuggestion> {
        if (tokens.isEmpty()) return emptyList()
        return context.offers
            .mapNotNull { offer ->
                val haystack = SearchTextNormalizer.normalizeSearchText(
                    buildString {
                        append(offer.titleFr).append(' ')
                        append(offer.titleAr).append(' ')
                        append(offer.company.name).append(' ')
                        append(offer.domain.nameFr).append(' ')
                        append(offer.domain.nameAr).append(' ')
                        append(offer.location.cityFr).append(' ')
                        append(offer.location.cityAr).append(' ')
                        offer.skills.forEach { s ->
                            append(s.nameFr).append(' ').append(s.nameAr).append(' ')
                        }
                    }
                )
                val hits = tokens.count { tok -> haystack.contains(tok) }
                if (hits > 0) {
                    val openBonus = if (offer.status == OfferStatus.OPEN && !offer.isExpired(context.todayIso)) 1 else 0
                    Triple(offer, hits, openBonus)
                } else {
                    null
                }
            }
            .sortedWith(compareByDescending<Triple<Offer, Int, Int>> { it.second }.thenByDescending { it.third }.thenByDescending { it.first.monthlyStipendDzd })
            .take(3)
            .map { (offer, _, _) -> offer.toSuggestion(context.language) }
    }

    private fun topOpenOffers(context: AssistantContext, limit: Int): List<AssistantOfferSuggestion> {
        return context.offers
            .filter { it.status == OfferStatus.OPEN && !it.isExpired(context.todayIso) }
            .sortedByDescending { it.monthlyStipendDzd }
            .take(limit)
            .map { it.toSuggestion(context.language) }
    }

    private fun Offer.toSuggestion(lang: AppLanguage): AssistantOfferSuggestion {
        val isAr = lang == AppLanguage.AR
        val title = if (isAr) titleAr else titleFr
        val city = if (isAr) location.cityAr else location.cityFr
        val stipend = if (isPaid && monthlyStipendDzd > 0) {
            tr(
                lang,
                fr = { "$monthlyStipendDzd DA/mois" },
                ar = { "$monthlyStipendDzd دج/شهر" },
                en = { "$monthlyStipendDzd DZD/mo" }
            )
        } else {
            tr(
                lang,
                fr = { "Non rémunéré" },
                ar = { "غير مدفوع" },
                en = { "Unpaid" }
            )
        }
        return AssistantOfferSuggestion(
            offerId = id,
            title = title,
            subtitle = "${company.name} · $city · $stipend"
        )
    }

    private fun followUps(context: AssistantContext, exclude: AssistantTopic): List<AssistantQuickQuestion> =
        quickQuestionsFor(context).filterNot { it.topic == exclude }.take(3)

    private fun shortcut(target: AssistantNavTarget, lang: AppLanguage): AssistantActionShortcut {
        val label = when (target) {
            AssistantNavTarget.OFFERS -> tr(lang, { "Aller aux Offres" }, { "الانتقال إلى العروض" }, { "Go to Offers" })
            AssistantNavTarget.FAVORITES -> tr(lang, { "Ouvrir les Favoris" }, { "فتح المفضلة" }, { "Open Favorites" })
            AssistantNavTarget.APPLICATIONS -> tr(lang, { "Voir mes Candidatures" }, { "فتح ترشحاتي" }, { "View Applications" })
            AssistantNavTarget.PROFILE -> tr(lang, { "Ouvrir Profil & Paramètres" }, { "فتح حسابي والإعدادات" }, { "Open Profile & Settings" })
        }
        return AssistantActionShortcut(label = label, target = target)
    }
}
