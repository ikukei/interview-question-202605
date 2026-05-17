import Foundation

/// Evaluation context passed by the calling app.
public struct FeatureContext {
    /// Unique identifier for the subject being evaluated (e.g. user ID).
    public let subjectKey: String
    /// Geographic region (e.g. "Asia", "North America").
    public let region: String?
    /// Subject group (e.g. "vip", "internal").
    public let subject: String?
    /// Release tag (e.g. "20260518").
    public let releaseKey: String?
    /// Additional free-form attributes forwarded to the backend.
    public let attributes: [String: String]

    public init(
        subjectKey: String,
        region: String? = nil,
        subject: String? = nil,
        releaseKey: String? = nil,
        attributes: [String: String] = [:]
    ) {
        self.subjectKey = subjectKey
        self.region = region
        self.subject = subject
        self.releaseKey = releaseKey
        self.attributes = attributes
    }
}
