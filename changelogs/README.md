One file per version, `<version>.md`, written by hand: the notes published
with that version. A version is not published without its file.

A version is published by pushing its tag, `v<version>` (or
`v<version>+<minecraft>` when the same version ships on several Minecraft
branches), on a commit of the version branch that holds its notes. Push at
most three tags at a time.
