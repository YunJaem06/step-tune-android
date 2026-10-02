package hs.project.steptune.feature.settings

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsUiStateTest {
    @Test
    fun `nickname can be updated only after availability succeeds`() {
        val unchecked = SettingsUiState(
            nickName = "기존닉네임",
            nicknameInput = "새닉네임"
        )
        val available = unchecked.copy(
            nicknameValidationState = NicknameValidationState.AVAILABLE
        )

        assertTrue(unchecked.canCheckNickname)
        assertFalse(unchecked.canUpdateNickname)
        assertTrue(available.canUpdateNickname)
    }

    @Test
    fun `same or too long nickname cannot be checked`() {
        val sameNickname = SettingsUiState(
            nickName = "같은닉네임",
            nicknameInput = " 같은닉네임 "
        )
        val tooLongNickname = sameNickname.copy(
            nicknameInput = "가".repeat(31)
        )

        assertFalse(sameNickname.canCheckNickname)
        assertFalse(tooLongNickname.isNicknameInputValid)
        assertFalse(tooLongNickname.canCheckNickname)
    }
}
