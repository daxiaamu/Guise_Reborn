package com.houvven.guise.ui.routing

import android.annotation.SuppressLint
import android.net.Uri
import androidx.activity.BackEventCompat
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.houvven.guise.db.Template
import com.houvven.guise.ui.routing.editor.AddTemplateScreen
import com.houvven.guise.ui.routing.editor.EditTemplateScreen
import com.houvven.guise.ui.routing.launcher.LauncherRoute
import com.houvven.guise.ui.routing.launcher.TemplateQrScannerScreen
import com.houvven.guise.ui.routing.template.EnableTemplateScreen
import com.houvven.guise.ui.theme.predictiveBack

@SuppressLint("StaticFieldLeak")
object LocalNavController {
    lateinit var current: NavHostController
}

@Composable
fun NavigationRoute() {
    val navController = rememberNavController()
    val predictiveBackEnabled by predictiveBack
    val activity = LocalActivity.current
    LocalNavController.current = navController
    BackHandler(
        enabled = !predictiveBackEnabled,
    ) {
        if (!navController.popBackStack()) activity?.finish()
    }

    NavHost(
        navController = navController,
        startDestination = NavRoutingTypes.LAUNCHER.name,
        enterTransition = {
            if (predictiveBackEnabled) {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(durationMillis = PREDICTIVE_BACK_DURATION_MILLIS),
                )
            } else {
                EnterTransition.None
            }
        },
        exitTransition = { ExitTransition.None },
        popEnterTransition = {
            if (predictiveBackEnabled) {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(
                        durationMillis = PREDICTIVE_BACK_DURATION_MILLIS,
                        easing = LinearEasing,
                    ),
                    initialOffset = { fullWidth ->
                        fullWidth / PREDICTIVE_BACK_BACKGROUND_OFFSET_DIVISOR
                    },
                ) + scaleIn(
                    initialScale = PREDICTIVE_BACK_BACKGROUND_MIN_SCALE,
                    animationSpec = tween(
                        durationMillis = PREDICTIVE_BACK_DURATION_MILLIS,
                        easing = LinearEasing,
                    ),
                ) + fadeIn(
                    initialAlpha = PREDICTIVE_BACK_BACKGROUND_MIN_ALPHA,
                    animationSpec = tween(
                        durationMillis = PREDICTIVE_BACK_DURATION_MILLIS,
                        easing = LinearEasing,
                    ),
                )
            } else {
                EnterTransition.None
            }
        },
        popExitTransition = {
            if (predictiveBackEnabled) {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(durationMillis = PREDICTIVE_BACK_DURATION_MILLIS),
                )
            } else {
                ExitTransition.None
            }
        },
        predictivePopEnterTransition = { swipeEdge ->
            if (predictiveBackEnabled) {
                val direction = if (swipeEdge == BackEventCompat.EDGE_RIGHT) {
                    AnimatedContentTransitionScope.SlideDirection.Right
                } else {
                    AnimatedContentTransitionScope.SlideDirection.Left
                }
                slideIntoContainer(
                    towards = direction,
                    animationSpec = tween(
                        durationMillis = PREDICTIVE_BACK_DURATION_MILLIS,
                        easing = LinearEasing,
                    ),
                    initialOffset = { fullWidth ->
                        fullWidth / PREDICTIVE_BACK_BACKGROUND_OFFSET_DIVISOR
                    },
                ) + scaleIn(
                    initialScale = PREDICTIVE_BACK_BACKGROUND_MIN_SCALE,
                    animationSpec = tween(
                        durationMillis = PREDICTIVE_BACK_DURATION_MILLIS,
                        easing = LinearEasing,
                    ),
                ) + fadeIn(
                    initialAlpha = PREDICTIVE_BACK_BACKGROUND_MIN_ALPHA,
                    animationSpec = tween(
                        durationMillis = PREDICTIVE_BACK_DURATION_MILLIS,
                        easing = LinearEasing,
                    ),
                )
            } else {
                EnterTransition.None
            }
        },
        predictivePopExitTransition = { swipeEdge ->
            if (predictiveBackEnabled) {
                slideOutOfContainer(
                    towards = if (swipeEdge == BackEventCompat.EDGE_RIGHT) {
                        AnimatedContentTransitionScope.SlideDirection.Right
                    } else {
                        AnimatedContentTransitionScope.SlideDirection.Left
                    },
                    animationSpec = tween(
                        durationMillis = PREDICTIVE_BACK_DURATION_MILLIS,
                        // Keep page displacement proportional to predictive-back progress.
                        easing = LinearEasing,
                    ),
                )
            } else {
                ExitTransition.None
            }
        },
    ) {
        composable(NavRoutingTypes.LAUNCHER.name) { LauncherRoute() }

        composable(NavRoutingTypes.ADD_TEMPLATE.name) { AddTemplateScreen() }

        composable(NavRoutingTypes.SCAN_TEMPLATE_QR.name) { TemplateQrScannerScreen() }

        composable(
            route = "${NavRoutingTypes.EDIT_TEMPLATE.name}/{template}",
            arguments = listOf(navArgument("template") { type = NavType.StringType })
        ) {
            val template = Template.deserialization(it.arguments!!.getString("template")!!)
            EditTemplateScreen(template)
        }

        composable(
            route = "${NavRoutingTypes.ENABLE_TEMPLATE.name}/{template}",
            arguments = listOf(navArgument("template") { type = NavType.StringType })
        ) {
            val template = Template.deserialization(it.arguments!!.getString("template")!!)
            EnableTemplateScreen(template)
        }
    }
}


fun NavHostController.navigateWithTemplate(route: String, template: Template) {
    navigate("$route/${Uri.encode(template.serialization())}")
}
