package com.rawedit.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.rawedit.app.ui.RawEditViewModel
import com.rawedit.app.ui.components.LiquidBottomDock
import com.rawedit.app.ui.components.MainDockTab
import com.rawedit.app.ui.screens.CreateFlowScreen
import com.rawedit.app.ui.screens.HomeScreen
import com.rawedit.app.ui.screens.KeyframeSheetReviewScreen
import com.rawedit.app.ui.screens.MyReelsScreen
import com.rawedit.app.ui.screens.OnboardingScreen
import com.rawedit.app.ui.screens.ProfileScreen
import com.rawedit.app.ui.screens.RenderingScreen
import com.rawedit.app.ui.screens.ResultScreen
import com.rawedit.app.ui.theme.RawColors
import com.rawedit.app.ui.theme.RawEditTheme
import com.rawedit.app.ui.theme.RawMotionConfig

class MainActivity : ComponentActivity() {

    private val viewModel: RawEditViewModel by viewModels()

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Optional notification permission handled gracefully */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermissionIfNeeded()

        setContent {
            val prefs by viewModel.userPrefs.collectAsStateWithLifecycle()
            val motionConfig = RawMotionConfig(
                reducedMotion = prefs.reducedMotion,
                hapticsEnabled = prefs.hapticsEnabled,
                isLowRamDevice = viewModel.deviceProfile.isLowRamDevice || prefs.ultraLowEndMode
            )

            RawEditTheme(motionConfig = motionConfig) {
                RawEditAppRoot(viewModel = viewModel)
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

@Composable
private fun RawEditAppRoot(viewModel: RawEditViewModel) {
    val navController = rememberNavController()
    val prefs by viewModel.userPrefs.collectAsStateWithLifecycle()
    val projects by viewModel.allProjects.collectAsStateWithLifecycle()
    val videos by viewModel.allVideos.collectAsStateWithLifecycle()
    val createState by viewModel.createState.collectAsStateWithLifecycle()
    val renderState by viewModel.renderState.collectAsStateWithLifecycle()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: MainDockTab.HOME.route

    if (!prefs.onboardingDone) {
        OnboardingScreen(
            cacheManager = viewModel.cacheManager,
            onComplete = { name, avatar ->
                viewModel.completeOnboarding(name, avatar)
            }
        )
        return
    }

    val showBottomDock = currentRoute in setOf(
        MainDockTab.HOME.route,
        MainDockTab.CREATE.route,
        MainDockTab.MY_REELS.route,
        MainDockTab.PROFILE.route
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(RawColors.Background)
            .systemBarsPadding()
    ) {
        NavHost(
            navController = navController,
            startDestination = MainDockTab.HOME.route,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            enterTransition = { morphEnterTransition(prefs.reducedMotion) },
            exitTransition = { morphExitTransition(prefs.reducedMotion) },
            popEnterTransition = { morphEnterTransition(prefs.reducedMotion) },
            popExitTransition = { morphExitTransition(prefs.reducedMotion) }
        ) {
            composable(MainDockTab.HOME.route) {
                HomeScreen(
                    projects = projects,
                    videos = videos,
                    onStartCreating = {
                        viewModel.startNewProject {
                            navController.navigate(MainDockTab.CREATE.route)
                        }
                    },
                    onOpenMyReels = {
                        navController.navigate(MainDockTab.MY_REELS.route)
                    }
                )
            }

            composable(MainDockTab.CREATE.route) {
                if (createState.project == null) {
                    viewModel.startNewProject()
                }
                CreateFlowScreen(
                    state = createState,
                    sampleIntervalMs = prefs.sampleIntervalMs,
                    viewModel = viewModel,
                    onOpenSheetReview = {
                        navController.navigate("sheet_review")
                    },
                    onLaunchRenderScreen = {
                        navController.navigate("rendering")
                    }
                )
            }

            composable(MainDockTab.MY_REELS.route) {
                MyReelsScreen(
                    projects = projects,
                    onStartCreating = {
                        viewModel.startNewProject {
                            navController.navigate(MainDockTab.CREATE.route)
                        }
                    },
                    onOpenProject = { proj ->
                        viewModel.loadProjectIntoFlow(proj.id)
                        navController.navigate(MainDockTab.CREATE.route)
                    },
                    onOpenResult = { proj ->
                        viewModel.loadProjectIntoFlow(proj.id)
                        navController.navigate("result/${proj.id}")
                    },
                    onOpenKeyframeSheet = { proj ->
                        viewModel.loadProjectIntoFlow(proj.id)
                        navController.navigate("sheet_review")
                    },
                    onRenameProject = viewModel::renameProject,
                    onDuplicateProject = viewModel::duplicateProject,
                    onDeleteProject = viewModel::deleteProject
                )
            }

            composable(MainDockTab.PROFILE.route) {
                ProfileScreen(
                    prefs = prefs,
                    deviceProfile = viewModel.deviceProfile,
                    projectCount = projects.size,
                    renderCount = videos.size,
                    cacheManager = viewModel.cacheManager,
                    onUpdateProfile = viewModel::updateProfile,
                    onToggleHaptics = viewModel::setHapticsEnabled,
                    onToggleReducedMotion = viewModel::setReducedMotion,
                    onToggleUltraLowEnd = viewModel::setUltraLowEndMode
                )
            }

            composable("sheet_review") {
                val context = androidx.compose.ui.platform.LocalContext.current
                KeyframeSheetReviewScreen(
                    state = createState,
                    onJumpToTimestampMs = { targetMs ->
                        viewModel.updatePreviewTimestampAndReport(targetMs, createState.autofitReport)
                        viewModel.setCreateStep(3)
                        navController.popBackStack()
                    },
                    onCopyFixPrompt = {
                        viewModel.copyTextToClipboard("RAW_EDIT_FIX_PROMPT", createState.fixPromptText)
                    },
                    onSharePdf = {
                        createState.latestSheet?.let {
                            viewModel.shareFilePath(context, it.pdfUri, "application/pdf", "Share Contact Sheet PDF")
                        }
                    },
                    onShareZip = {
                        createState.latestSheet?.let {
                            viewModel.shareFilePath(context, it.zipUri, "application/zip", "Share Keyframes ZIP")
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable("rendering") {
                val context = androidx.compose.ui.platform.LocalContext.current
                RenderingScreen(
                    renderState = renderState,
                    onCancel = {
                        viewModel.cancelActiveRender(context)
                        navController.popBackStack()
                    },
                    onFinished = {
                        val pid = renderState.projectId.ifBlank { createState.project?.id ?: "" }
                        navController.navigate("result/$pid") {
                            popUpTo(MainDockTab.CREATE.route) { inclusive = false }
                        }
                    }
                )
            }

            composable("result/{projectId}") { entry ->
                val context = androidx.compose.ui.platform.LocalContext.current
                val pid = entry.arguments?.getString("projectId") ?: createState.project?.id ?: ""
                val matchedVideo = videos.firstOrNull { it.projectId == pid } ?: videos.firstOrNull()
                val projTitle = projects.firstOrNull { it.id == pid }?.title ?: createState.project?.title ?: "FINISHED REEL"

                ResultScreen(
                    video = matchedVideo,
                    projectTitle = projTitle,
                    onShareVideo = { path ->
                        viewModel.shareFilePath(context, path, "video/mp4", "Share 1080p30 Reel MP4")
                    },
                    onReRender = {
                        viewModel.startFinalRender(context)
                        navController.navigate("rendering")
                    },
                    onOpenKeyframeSheet = {
                        navController.navigate("sheet_review")
                    },
                    onEditPromptAndRetry = {
                        viewModel.setCreateStep(1)
                        navController.navigate(MainDockTab.CREATE.route)
                    },
                    onBackToMyReels = {
                        navController.navigate(MainDockTab.MY_REELS.route)
                    }
                )
            }
        }

        if (showBottomDock) {
            LiquidBottomDock(
                currentRoute = currentRoute,
                onSelectTab = { tab ->
                    if (tab == MainDockTab.CREATE && createState.project == null) {
                        viewModel.startNewProject()
                    }
                    navController.navigate(tab.route) {
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.morphEnterTransition(
    reducedMotion: Boolean
): EnterTransition {
    if (reducedMotion) return fadeIn()
    return scaleIn(
        initialScale = 0.92f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        )
    ) + fadeIn(
        animationSpec = spring(stiffness = Spring.StiffnessMedium)
    )
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.morphExitTransition(
    reducedMotion: Boolean
): ExitTransition {
    if (reducedMotion) return fadeOut()
    return scaleOut(
        targetScale = 0.95f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        )
    ) + fadeOut(
        animationSpec = spring(stiffness = Spring.StiffnessMedium)
    )
}
