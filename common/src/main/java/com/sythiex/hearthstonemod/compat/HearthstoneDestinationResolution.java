package com.sythiex.hearthstonemod.compat;

import java.util.Objects;

import net.minecraft.network.chat.Component;

/**
 * Server-thread preparation and loading lease, closed after cancellation or arrival support expires.
 */
public interface HearthstoneDestinationResolution extends AutoCloseable
{
    String UNAVAILABLE_MESSAGE = "item.hearthstone.destination_unavailable";

    /**
     * Polls preparation without blocking on later server ticks.
     */
    Result poll();

    /**
     * Releases loading resources; repeated calls must be harmless.
     */
    @Override
    default void close()
    {}

    /**
     * Creates a completed resolution with no resources to release.
     */
    static HearthstoneDestinationResolution ready(HearthstoneDestination destination)
    {
        Result result = Result.ready(destination);
        return () -> result;
    }

    /**
     * Creates a failed resolution that must never fall back to stored coordinates.
     */
    static HearthstoneDestinationResolution unavailable(String messageKey)
    {
        Result result = Result.unavailable(messageKey);
        return () -> result;
    }

    /**
     * Preparation outcomes keep an unloaded destination distinct from a missing block.
     */
    enum Status { PENDING, READY, UNAVAILABLE }

    /**
     * Only READY carries a destination; only UNAVAILABLE carries a message.
     */
    record Result(Status status, HearthstoneDestination destination, Component message)
    {
        public Result
        {
            Objects.requireNonNull(status);
            if ((status == Status.READY) != (destination != null) || (status == Status.UNAVAILABLE) != (message != null))
            {
                throw new IllegalArgumentException("Invalid destination resolution result");
            }
        }

        public static Result pending()
        {
            return new Result(Status.PENDING, null, null);
        }

        public static Result ready(HearthstoneDestination destination)
        {
            return new Result(Status.READY, Objects.requireNonNull(destination), null);
        }

        public static Result unavailable(String messageKey)
        {
            return new Result(Status.UNAVAILABLE, null, Component.translatable(messageKey));
        }
    }
}
