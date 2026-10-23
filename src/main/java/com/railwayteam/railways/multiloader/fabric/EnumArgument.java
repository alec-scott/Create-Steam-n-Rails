/*
 * Steam 'n' Rails
 * Copyright (c) 2022-2024 The Railways Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package com.railwayteam.railways.multiloader.fabric;

import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.railwayteam.railways.Railways;
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/**
 * A command argument matching an enum constant by name.
 *
 * <p>PORT-NOTE(fabric): Porting Lib's {@code EnumArgument} (from NeoForge) has no 26.1 release. Same design: the enum
 * class name is sent to clients so they can parse and suggest it.</p>
 */
public class EnumArgument<T extends Enum<T>> implements ArgumentType<T> {
	private static final Dynamic2CommandExceptionType INVALID_ENUM = new Dynamic2CommandExceptionType(
		(found, constants) -> Component.literal("Invalid enum value '" + found + "', expected one of: " + constants));

	private final Class<T> enumClass;

	public static <R extends Enum<R>> EnumArgument<R> enumArgument(Class<R> enumClass) {
		return new EnumArgument<>(enumClass);
	}

	private EnumArgument(Class<T> enumClass) {
		this.enumClass = enumClass;
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	public static void register() {
		ArgumentTypeRegistry.registerArgumentType(Railways.asResource("enum"), (Class) EnumArgument.class, (ArgumentTypeInfo) new Info());
	}

	@Override
	public T parse(StringReader reader) throws CommandSyntaxException {
		String name = reader.readUnquotedString();
		try {
			return Enum.valueOf(enumClass, name);
		} catch (IllegalArgumentException e) {
			throw INVALID_ENUM.createWithContext(reader, name, Arrays.toString(enumClass.getEnumConstants()));
		}
	}

	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
		return SharedSuggestionProvider.suggest(Stream.of(enumClass.getEnumConstants()).map(Enum::name), builder);
	}

	@Override
	public Collection<String> getExamples() {
		return Stream.of(enumClass.getEnumConstants()).map(Enum::name).toList();
	}

	public static class Info<T extends Enum<T>> implements ArgumentTypeInfo<EnumArgument<T>, Info<T>.Template> {
		@Override
		public void serializeToNetwork(Template template, FriendlyByteBuf buffer) {
			buffer.writeUtf(template.enumClass.getName());
		}

		@Override
		@SuppressWarnings("unchecked")
		public Template deserializeFromNetwork(FriendlyByteBuf buffer) {
			try {
				return new Template((Class<T>) Class.forName(buffer.readUtf()));
			} catch (ClassNotFoundException e) {
				return null;
			}
		}

		@Override
		public void serializeToJson(Template template, JsonObject json) {
			json.addProperty("enum", template.enumClass.getName());
		}

		@Override
		public Template unpack(EnumArgument<T> argument) {
			return new Template(argument.enumClass);
		}

		public class Template implements ArgumentTypeInfo.Template<EnumArgument<T>> {
			final Class<T> enumClass;

			Template(Class<T> enumClass) {
				this.enumClass = enumClass;
			}

			@Override
			public EnumArgument<T> instantiate(CommandBuildContext context) {
				return new EnumArgument<>(enumClass);
			}

			@Override
			public ArgumentTypeInfo<EnumArgument<T>, ?> type() {
				return Info.this;
			}
		}
	}
}
