package org.anjisuan608.hihonor.ai_freegrip.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.annotation.StringRes
import org.anjisuan608.hihonor.ai_freegrip.R
import org.anjisuan608.hihonor.ai_freegrip.ui.theme.AIFreegripTheme

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
 * 合规披露页：展示第 3 节要求的全部披露字段 + 本应用不收集信息的说明。
 * 从主界面顶栏或底部入口可达（AGENTS.md P3）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComplianceScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.compliance_title)) },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text(stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(innerPadding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = 16.dp,
                vertical = 12.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
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
                Card(modifier = Modifier.fillMaxWidth()) {
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
        }
    }
}

@Preview(showBackground = true, name = "合规披露页")
@Composable
private fun ComplianceScreenPreview() {
    AIFreegripTheme {
        ComplianceScreen(onBack = {})
    }
}
