package org.bukkit.util;

import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

public class MessageUtils {

	public static final MiniMessage SKRIPT_MINI_MESSAGE = MiniMessage.builder()
		.postProcessor(component -> component.compact().decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE))
		.tags(TagResolver.resolver(
			TagResolver.standard(),
			TagResolver.resolver("error_color", Tag.styling(TextColor.color(0xFF7E66))),
			TagResolver.resolver("success_color", Tag.styling(TextColor.color(0x66FF96))),
			TagResolver.resolver("base_grey", Tag.styling(TextColor.color(0xCCC4C4))),
			Placeholder.parsed("skript_minestom_tag", "<dark_gray>[</dark_gray><gradient:#ff6c2f:#ff76b6>Skript-Minestom</gradient><dark_gray>]</dark_gray>")
		)).build();

}
