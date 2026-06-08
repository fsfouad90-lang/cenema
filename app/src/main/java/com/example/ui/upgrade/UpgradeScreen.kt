package com.example.ui.upgrade

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.example.R
import com.example.CinemaApplication
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun UpgradeScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as CinemaApplication
    val subscriptionRepository = app.subscriptionRepository
    val authRepository = app.authRepository
    val userSessionState by authRepository.currentUserSession.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    var isUpgrading by remember { mutableStateOf(false) }
    var isSuccess by remember { mutableStateOf(false) }
    var selectedPlanIndex by remember { mutableIntStateOf(0) } // 0 = Monthly, 1 = Yearly

    val currentLang = if (Locale.getDefault().language.startsWith("ar")) "ar" else "en"
    val isAr = currentLang == "ar"

    val headerTitle = if (isAr) "انضم للمميزين في سينما فلو" else "Unlock CinemaFlow Premium"
    val headerSubtitle = if (isAr) "استمتع بمشاهدة تدفق سينمائي بلا حدود وبدون إعلانات" else "Stream your favorite Hollywood titles with zero limitations"
    
    val benefits = if (isAr) {
        listOf(
            "وصول كامل إلى جميع الأفلام والمسلسلات الحصرية",
            "أعلى جودة صوت وصورة فائقة الدقة بالكامل Ultra HD",
            "لا توجد إعلانات منبثقة أو مزعجة أثناء العرض",
            "إمكانية تنزيل الأفلام للمشاهدة دون إنترنت"
        )
    } else {
        listOf(
            "Instant access to 100% of the cinematic catalog",
            "Stunning full Ultra HD high bitrate visual quality",
            "Absolutely zero commercial interruptions or ads",
            "Offline playback down-loads securely synced"
        )
    }

    val plans = listOf(
        PlanItem(
            title = if (isAr) "العضوية الشهرية" else "Monthly Membership",
            price = if (isAr) "4.99 $ / شهرياً" else "$4.99 / Month",
            description = if (isAr) "اشتراك مرن، الإلغاء في أي وقت" else "Flexible, cancel anytime"
        ),
        PlanItem(
            title = if (isAr) "العضوية السنوية" else "Annual Membership",
            price = if (isAr) "39.99 $ / سنوياً" else "$39.99 / Year",
            description = if (isAr) "وفر 33% برأس مال منخفض" else "Save 33% with long term focus"
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CinemaBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            // Elegant Back Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(CinemaSurface)
                        .testTag("upgrade_back_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Navigate Back",
                        tint = CinemaTextLight
                    )
                }
            }

            // High aesthetic premium dynamic backdrop decoration header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                IndigoPrimary,
                                PurpleAccent,
                                CinemaGold.copy(alpha = 0.5f)
                            )
                        )
                    )
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = CinemaGold,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = headerTitle,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = headerSubtitle,
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Benefit checkpoints
            benefits.forEach { benefit ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(CinemaGold.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = CinemaGold,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Text(
                        text = benefit,
                        color = CinemaTextLight,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Pricing details grids (spacious touch targets > 48dp)
            plans.forEachIndexed { index, plan ->
                val selected = selectedPlanIndex == index
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (selected) IndigoPrimary.copy(alpha = 0.15f) else CinemaSurface)
                        .border(
                            width = 2.dp,
                            color = if (selected) CinemaGold else Color.Transparent,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable { selectedPlanIndex = index }
                        .padding(20.dp)
                        .testTag("plan_card_$index")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = plan.title,
                                color = if (selected) CinemaGold else CinemaTextLight,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = plan.description,
                                color = CinemaTextMuted,
                                fontSize = 12.sp
                            )
                        }
                        Text(
                            text = plan.price,
                            color = CinemaTextLight,
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Subscription trigger button (touch target >= 48dp)
            Button(
                onClick = {
                    isUpgrading = true
                    coroutineScope.launch {
                        delay(1200) // Simulate checking gateway
                        val userId = when (val session = userSessionState) {
                            is com.example.data.model.UserSession.LoggedIn -> session.userId
                            else -> "current_user_id"
                        }
                        subscriptionRepository.updateSubscriptionStatus(userId, "Premium")
                        isUpgrading = false
                        isSuccess = true
                        delay(2000)
                        onBack() // pop back to Movie Details as Premium!
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("submit_upgrade_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CinemaGold,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(14.dp),
                enabled = !isUpgrading && !isSuccess
            ) {
                if (isUpgrading) {
                    CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
                } else if (isSuccess) {
                    Text(
                        text = if (isAr) "تمت الترقية بنجاح! 🎉" else "Success! Premium Active 🎉",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                } else {
                    Text(
                        text = if (isAr) "ادفع الآن واستمتع" else "Checkout and Subscribe",
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

private data class PlanItem(
    val title: String,
    val price: String,
    val description: String
)
