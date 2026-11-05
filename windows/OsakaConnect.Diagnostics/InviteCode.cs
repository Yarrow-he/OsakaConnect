namespace OsakaConnect.Diagnostics;

public static class InviteCode
{
    private const string Alphabet = "0123456789ABCDEFGHJKMNPQRSTVWXYZ";

    public static bool IsValid(string? value)
    {
        if (value is null)
            return false;

        var trimmed = value.Trim();
        if (trimmed.Length != 8)
            return false;

        foreach (var character in trimmed)
        {
            // Reject non-ASCII before casing, which can map Unicode letters to ASCII.
            if (character > 127 || !Alphabet.Contains(char.ToUpperInvariant(character)))
                return false;
        }

        return true;
    }
}
