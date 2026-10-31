// Part of the Converge SDK runtime. Synced with TANTIRA/CSDK-Java
// (dev.converge:converge-sdk); edit there or here and mirror the other.
package dev.converge.sdk;

import java.util.List;

public record Page<T>(List<T> data, String nextPageToken, String totalCount) {}
