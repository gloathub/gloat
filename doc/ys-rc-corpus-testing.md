Testing gloat's let-go (lg) engine against the RosettaCode YS corpus
=====================================================================

Needs: git, curl, make, bash (Linux or macOS).

The tester is part of the gloat repo: `util/ys-rc-corpus-testing`.
Get the repo and run the one-time setup:

    git clone -b lg-spike https://github.com/gloathub/gloat
    cd gloat

    # Clones let-go (gloathub fork) and the RosettaCode corpus (rcd)
    # NEXT TO the gloat repo dir, then bootstraps the toolchain and
    # builds lg from the let-go checkout. Expect a few minutes on
    # first run.
    util/ys-rc-corpus-testing

The setup finds the repo via its .git/ dir, so it also works from a
git worktree; let-go/ and rcd/ always land beside the main repo dir.

Run one program:

    # An example of something where lg takes 40 seconds:
    echo ../rcd/Lang/YAMLScript/Arithmetic-numbers/arithmetic-numbers.ys |
      YS=1 TIMEOUT=0 util/ys-rc-corpus-testing

Run the whole corpus (~710 programs):

    find ../rcd -name '*.ys' | sort | util/ys-rc-corpus-testing

All output tees to ./out in the current directory.
Programs that pass are copied to good/, ones that fail to fail/, and
ones that exceed the timeout to long/, so after a sweep the fail/ and
long/ directories are the interesting bits.

Options (environment variables):

    COUNT=<num>    Number of files to test
    SHUFFLE=1      Shuffle the file list first
    TIMEOUT=<sec>  Per-test timeout (default 2, 0 for none)
    JOLT=1         Run with jolt instead of gloat -Elgvm
    BB=1           Run with bb (babashka) instead of gloat -Elgvm
    GLOAT=1        With JOLT=1 or BB=1, also run gloat -Elgvm
    YS=1           Also run each test with ys (timed, for comparison)
    PAUSE=1        Pause after each test

Example:

    find ../rcd -name '*.ys' |
      SHUFFLE=1 COUNT=10 YS=1 util/ys-rc-corpus-testing

Note: the gloat timing line is run-only (compilation excluded), while
the ys timing is the whole invocation including its compile step.
