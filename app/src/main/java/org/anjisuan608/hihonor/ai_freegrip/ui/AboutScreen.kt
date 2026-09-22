package org.anjisuan608.hihonor.ai_freegrip.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.anjisuan608.hihonor.ai_freegrip.R
import org.anjisuan608.hihonor.ai_freegrip.ui.theme.AIFreegripTheme
import org.anjisuan608.hihonor.ai_freegrip.ui.theme.oledModuleBorder

/** 荣耀 AI FreeGrip 官方开发者文档（Introduction 页）。 */
private const val SDK_DOC_URL = "https://developer.honor.com/cn/docs/aifreegrip/guides/introduction"

/** GitHub 仓库地址（README 视图）。 */
private const val REPO_URL = "https://github.com/anjisuan608/AI-Freegrip?tab=readme-ov-file"

/** MIT 许可证全文。 */
private const val LICENSE_URL = "https://github.com/anjisuan608/AI-Freegrip/blob/main/LICENSE"

/** 一条披露字段：label + value 均引用 strings.xml（合规文案集中可审）。 */
private data class DisclosureField(@param:StringRes val label: Int, @param:StringRes val value: Int)

/**
 * 官方《SDK 合规使用说明》2.2 要求披露的 7 个字段，顺序与措辞照抄，
 * 「不涉及」的三项共用同一个字符串资源。
 */
private val disclosureFields = listOf(
    DisclosureField(R.string.label_company, R.string.value_company),
    DisclosureField(R.string.label_sdk_name, R.string.value_sdk_name),
    DisclosureField(R.string.label_purpose, R.string.value_purpose),
    DisclosureField(R.string.label_scene, R.string.value_scene),
    DisclosureField(R.string.label_personal_info, R.string.value_not_involved),
    DisclosureField(R.string.label_permission, R.string.value_not_involved),
    DisclosureField(R.string.label_privacy_link, R.string.value_not_involved),
)

/**
 * 关于页：
 * 1. 合规披露——原 [ComplianceScreen] 的全部内容（AGENTS.md 第 3 节要求的 7 字段）；
 * 2. SDK 相关链接——跳转 HONOR 开发者文档；
 * 3. 开源信息——作者 anjisuan608、MIT 许可证、GitHub 仓库。
 *
 * 顶栏与底部导航由 MainActivity 的外层 Scaffold 统一提供。
 */
@Composable
fun AboutScreen(modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // ---- 合规披露 ----
        item {
            SectionTitle(stringResource(R.string.compliance_title))
            Text(
                text = stringResource(R.string.compliance_intro),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        items(disclosureFields) { field ->
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = stringResource(field.label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(field.value),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth(), border = oledModuleBorder()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = stringResource(R.string.compliance_sdk_version),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = stringResource(R.string.compliance_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // ---- SDK 相关链接 ----
        item {
            SectionTitle(stringResource(R.string.about_section_links))
        }
        item {
            LinkCard(
                title = stringResource(R.string.about_link_sdk_doc),
                subtitle = SDK_DOC_URL,
                onClick = { uriHandler.openUri(SDK_DOC_URL) },
            )
        }

        // ---- 开源信息 ----
        item {
            SectionTitle(stringResource(R.string.about_section_open_source))
        }
        item {
            Card(modifier = Modifier.fillMaxWidth(), border = oledModuleBorder()) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    // 作者（纯文本）
                    LabeledValue(
                        label = stringResource(R.string.about_label_author),
                        value = stringResource(R.string.about_value_author),
                    )
                    // 许可证（可点击 → LICENSE 全文）
                    LabeledValue(
                        label = stringResource(R.string.about_label_license),
                        value = stringResource(R.string.about_value_license),
                        onClick = { uriHandler.openUri(LICENSE_URL) },
                    )
                    // 仓库（可点击 → GitHub）
                    LabeledValue(
                        label = stringResource(R.string.about_label_repo),
                        value = stringResource(R.string.about_value_repo),
                        onClick = { uriHandler.openUri(REPO_URL) },
                    )
                }
            }
        }

        item {
            Text(
                text = stringResource(R.string.footer_text),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
            )
        }
    }
}

/** 小节标题。 */
@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 6.dp),
    )
}

/** 独立的可点击链接卡片（SDK 文档入口）。 */
@Composable
private fun LinkCard(title: String, subtitle: String, onClick: () -> Unit) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** 标签 + 值一行；传 [onClick] 时值可点击（链接语义，主色显示）。 */
@Composable
private fun LabeledValue(label: String, value: String, onClick: (() -> Unit)? = null) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = if (onClick != null) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            modifier = if (onClick != null) {
                Modifier.clickable(onClick = onClick)
            } else {
                Modifier
            },
        )
    }
}

@Preview(showBackground = true, name = "关于页")
@Composable
private fun AboutScreenPreview() {
    AIFreegripTheme {
        AboutScreen()
    }
}
