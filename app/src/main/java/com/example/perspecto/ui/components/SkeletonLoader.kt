package com.example.perspecto.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

fun Modifier.skeletonEffect(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "skeleton_shimmer")
    val translateAnimation = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "skeleton_shimmer_translation"
    )

    val shimmerColors = listOf(
        Color.LightGray.copy(alpha = 0.6f),
        Color.LightGray.copy(alpha = 0.2f),
        Color.LightGray.copy(alpha = 0.6f),
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnimation.value, y = translateAnimation.value)
    )

    this.background(brush)
}

@Composable
fun VideoCardSkeleton() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = false) {} // Disable click effect
            .clickable(enabled = false) {}, // Disable click effect
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
        ) {
            // Thumbnail Placeholder (Left)
            Box(
                modifier = Modifier
                    .width(140.dp)
                    .fillMaxHeight()
                    .skeletonEffect()
            )

            // Content Area (Right)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(12.dp),
                verticalArrangement = Arrangement.Center
            ) {
                // Title Placeholder
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(20.dp) // Approximate TitleMedium height
                        .clip(RoundedCornerShape(4.dp))
                        .skeletonEffect()
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Chip Placeholder (Annotation Count)
                Box(
                    modifier = Modifier
                        .width(100.dp) // Approximate chip width
                        .height(32.dp) // Standard AssistChip height
                        .clip(RoundedCornerShape(8.dp)) // Chip shape
                        .skeletonEffect()
                )
            }
        }
    }
}

@Composable
fun AnnotationCardSkeleton() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp), // Match AnnotationCard height
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                // Content Placeholder (2 lines)
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .skeletonEffect()
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .skeletonEffect()
                )

                Spacer(modifier = Modifier.weight(1f))

                // Footer Row (Metadata)
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Time/Frame Placeholder
                    Box(
                        modifier = Modifier
                            .width(80.dp)
                            .height(24.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .skeletonEffect()
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // Comment Count Badge Placeholder
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(24.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .skeletonEffect()
                    )
                }
            }

            // Priority Badge Placeholder (Top Right)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 16.dp, end = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(24.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .skeletonEffect()
                )
            }
        }
    }
}
